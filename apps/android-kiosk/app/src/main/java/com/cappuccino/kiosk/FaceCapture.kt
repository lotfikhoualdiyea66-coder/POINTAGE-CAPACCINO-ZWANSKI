package com.cappuccino.kiosk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.sqrt

enum class FaceCaptureMode { VERIFY, ENROLL }

/** Guidance shown to the employee while the liveness sequence runs. */
enum class FaceHint { STARTING, NO_FACE, MANY_FACES, TOO_FAR, LOOK_STRAIGHT, TURN_HEAD, LOOK_BACK, DONE }

private const val CAPTURE_TIMEOUT_MS = 30_000L
private const val FRONTAL_MIN = 0.38f
private const val FRONTAL_MAX = 0.62f
private const val TURNED_LOW = 0.30f
private const val TURNED_HIGH = 0.70f
private const val MIN_FACE_WIDTH = 0.22f
private const val SAME_PERSON_MIN_COSINE = 0.5f

internal fun ImageProxy.toBgrMat(): Mat {
    val plane = planes.first()
    val source = plane.buffer.duplicate()
    val rowBytes = width * plane.pixelStride
    val rgbaBytes = ByteArray(width * height * 4)
    for (row in 0 until height) {
        source.position(row * plane.rowStride)
        source.get(rgbaBytes, row * width * 4, rowBytes)
    }

    val rgba = Mat(height, width, CvType.CV_8UC4)
    rgba.put(0, 0, rgbaBytes)
    val oriented = Mat()
    when (imageInfo.rotationDegrees) {
        90 -> Core.rotate(rgba, oriented, Core.ROTATE_90_CLOCKWISE)
        180 -> Core.rotate(rgba, oriented, Core.ROTATE_180)
        270 -> Core.rotate(rgba, oriented, Core.ROTATE_90_COUNTERCLOCKWISE)
        else -> rgba.copyTo(oriented)
    }
    val bgr = Mat()
    Imgproc.cvtColor(oriented, bgr, Imgproc.COLOR_RGBA2BGR)
    rgba.release()
    oriented.release()
    return bgr
}

private fun cosine(left: FloatArray, right: FloatArray): Float {
    var dot = 0f
    var leftNorm = 0f
    var rightNorm = 0f
    for (index in left.indices) {
        dot += left[index] * right[index]
        leftNorm += left[index] * left[index]
        rightNorm += right[index] * right[index]
    }
    return if (leftNorm == 0f || rightNorm == 0f) 0f else dot / (sqrt(leftNorm) * sqrt(rightNorm))
}

/**
 * Active liveness: a frontal capture, a visible head turn, then frontal capture(s) again.
 * A flat photo cannot change where the nose sits between the eyes, so it never passes the turn step.
 * Frames and embeddings stay in memory; only the final embeddings are handed back.
 */
private class FaceLivenessAnalyzer(
    private val context: Context,
    private val mainExecutor: Executor,
    private val mode: FaceCaptureMode,
    private val onHint: (FaceHint) -> Unit,
    private val onComplete: (List<FloatArray>) -> Unit,
    private val onError: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    private enum class Phase { FIRST_FRONTAL, TURN, SECOND_FRONTAL, FINISHED }

    private var engine: OpenCvFaceEngine? = null
    private var phase = Phase.FIRST_FRONTAL
    private val captures = mutableListOf<FloatArray>()
    private var lastAnalysisAt = 0L
    private var lastCaptureAt = 0L
    private var lastHint: FaceHint? = null
    private val capturesAfterTurn = if (mode == FaceCaptureMode.ENROLL) 2 else 1

    override fun analyze(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (phase == Phase.FINISHED || now - lastAnalysisAt < 150) {
            image.close()
            return
        }
        lastAnalysisAt = now

        try {
            val faceEngine = engine ?: OpenCvFaceEngine(context).also { engine = it }
            val wantsEmbedding = phase != Phase.TURN && now - lastCaptureAt > 350
            val frame = image.toBgrMat()
            val observation = try {
                faceEngine.observe(frame, withEmbedding = wantsEmbedding)
            } finally {
                frame.release()
            }
            step(observation, now)
        } catch (error: Exception) {
            phase = Phase.FINISHED
            KioskDiagnostics.record(context, "face_capture_error", "error_type=${error.javaClass.simpleName}")
            mainExecutor.execute { onError(error.message ?: error.javaClass.simpleName) }
        } finally {
            image.close()
        }
    }

    private fun step(observation: FaceObservation, now: Long) {
        when {
            observation.faceCount == 0 -> return hint(FaceHint.NO_FACE)
            observation.faceCount > 1 -> return hint(FaceHint.MANY_FACES)
            observation.sizeRatio < MIN_FACE_WIDTH -> return hint(FaceHint.TOO_FAR)
        }
        val frontal = observation.yawRatio in FRONTAL_MIN..FRONTAL_MAX
        when (phase) {
            Phase.FIRST_FRONTAL -> {
                val embedding = observation.embedding
                if (!frontal || embedding == null) return hint(FaceHint.LOOK_STRAIGHT)
                captures += embedding
                lastCaptureAt = now
                phase = Phase.TURN
                hint(FaceHint.TURN_HEAD)
            }
            Phase.TURN -> {
                if (observation.yawRatio < TURNED_LOW || observation.yawRatio > TURNED_HIGH) {
                    phase = Phase.SECOND_FRONTAL
                    hint(FaceHint.LOOK_BACK)
                } else {
                    hint(FaceHint.TURN_HEAD)
                }
            }
            Phase.SECOND_FRONTAL -> {
                val embedding = observation.embedding
                if (!frontal || embedding == null) return hint(FaceHint.LOOK_BACK)
                // The person after the turn must be the person before it.
                if (cosine(captures.first(), embedding) < SAME_PERSON_MIN_COSINE) {
                    captures.clear()
                    phase = Phase.FIRST_FRONTAL
                    return hint(FaceHint.LOOK_STRAIGHT)
                }
                captures += embedding
                lastCaptureAt = now
                if (captures.size >= 1 + capturesAfterTurn) {
                    phase = Phase.FINISHED
                    val result = captures.toList()
                    hint(FaceHint.DONE)
                    mainExecutor.execute { onComplete(result) }
                }
            }
            Phase.FINISHED -> Unit
        }
    }

    private fun hint(value: FaceHint) {
        if (value != lastHint) {
            lastHint = value
            mainExecutor.execute { onHint(value) }
        }
    }
}

private fun hintText(isArabic: Boolean, hint: FaceHint): String = when (hint) {
    FaceHint.STARTING -> if (isArabic) "جارٍ تشغيل الكاميرا…" else "Démarrage de la caméra…"
    FaceHint.NO_FACE -> if (isArabic) "ضع وجهك أمام الكاميرا" else "Placez votre visage devant la caméra"
    FaceHint.MANY_FACES -> if (isArabic) "شخص واحد فقط أمام الكاميرا" else "Une seule personne devant la caméra"
    FaceHint.TOO_FAR -> if (isArabic) "اقترب من الكاميرا" else "Approchez-vous de la caméra"
    FaceHint.LOOK_STRAIGHT -> if (isArabic) "انظر مباشرة إلى الكاميرا" else "Regardez droit vers la caméra"
    FaceHint.TURN_HEAD -> if (isArabic) "أدر رأسك قليلاً إلى اليمين أو اليسار" else "Tournez légèrement la tête à gauche ou à droite"
    FaceHint.LOOK_BACK -> if (isArabic) "انظر إلى الكاميرا مجدداً" else "Regardez à nouveau la caméra"
    FaceHint.DONE -> if (isArabic) "تم ✓" else "C’est bon ✓"
}

/** Camera panel for the kiosk card: runs the liveness sequence and returns the captured embeddings. */
@Composable
fun FaceCapturePanel(
    isArabic: Boolean,
    mode: FaceCaptureMode,
    errorMessage: String?,
    onComplete: (List<FloatArray>) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        permissionDenied = !granted
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Text(
        text = when (mode) {
            FaceCaptureMode.VERIFY -> if (isArabic) "تأكيد الوجه" else "Vérification du visage"
            FaceCaptureMode.ENROLL -> if (isArabic) "تسجيل وجهك" else "Enregistrement de votre visage"
        },
        color = Color(0xFFF6EBD7),
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(10.dp))
    errorMessage?.let {
        Text(it, color = Color(0xFFE49A8B), fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
    }

    if (hasPermission) {
        FaceCameraView(isArabic = isArabic, mode = mode, onComplete = onComplete, onTimeout = onCancel)
    } else {
        Text(
            if (permissionDenied) {
                if (isArabic) "إذن الكاميرا مرفوض. اسمح بالكاميرا في إعدادات الهاتف." else "Caméra refusée. Autorisez la caméra dans les réglages du téléphone."
            } else {
                if (isArabic) "يرجى السماح باستخدام الكاميرا." else "Autorisez l’accès à la caméra."
            },
            color = Color(0xFFC1B5A3),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
    Spacer(Modifier.height(6.dp))
    TextButton(onClick = onCancel) { Text(if (isArabic) "إلغاء" else "Annuler") }
    Text(
        if (isArabic) "تتم المعالجة على الهاتف. لا تُحفظ أي صورة." else "Traitement sur le téléphone. Aucune photo n’est enregistrée.",
        color = Color(0xFFC1B5A3),
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun FaceCameraView(
    isArabic: Boolean,
    mode: FaceCaptureMode,
    onComplete: (List<FloatArray>) -> Unit,
    onTimeout: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember(context) { ProcessCameraProvider.getInstance(context) }
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var hint by remember { mutableStateOf(FaceHint.STARTING) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        delay(CAPTURE_TIMEOUT_MS)
        KioskDiagnostics.record(context, "face_capture_timeout", "mode=$mode")
        onTimeout()
    }

    DisposableEffect(cameraProviderFuture, lifecycleOwner) {
        KioskDiagnostics.record(context, "face_capture_started", "mode=$mode")
        var cameraProvider: ProcessCameraProvider? = null
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                analysis.setAnalyzer(
                    analysisExecutor,
                    FaceLivenessAnalyzer(
                        context.applicationContext,
                        mainExecutor,
                        mode,
                        onHint = { hint = it },
                        onComplete = { embeddings ->
                            KioskDiagnostics.record(context, "face_capture_completed", "mode=$mode captures=${embeddings.size}")
                            onComplete(embeddings)
                        },
                        onError = { cameraError = it },
                    ),
                )
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
            } catch (error: Exception) {
                KioskDiagnostics.record(context, "face_camera_unavailable", "error_type=${error.javaClass.simpleName}")
                cameraError = error.message ?: error.javaClass.simpleName
            }
        }, mainExecutor)

        onDispose {
            cameraProvider?.unbindAll()
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(16.dp)),
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = cameraError?.let { if (isArabic) "خطأ في الكاميرا: $it" else "Erreur caméra : $it" } ?: hintText(isArabic, hint),
        color = if (cameraError != null) Color(0xFFE49A8B) else Color(0xFFD6B57A),
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
}
