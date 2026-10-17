package com.cappuccino.kiosk

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun FaceCameraDiagnosticsScreen(isArabic: Boolean, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val analysisExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember(context) { ProcessCameraProvider.getInstance(context) }
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var faceCount by remember { mutableIntStateOf(0) }
    var cameraError by remember { mutableStateOf("") }
    var isReady by remember { mutableStateOf(false) }
    var hasAnalyzedFrame by remember { mutableStateOf(false) }

    DisposableEffect(cameraProviderFuture, lifecycleOwner) {
        KioskDiagnostics.record(context, "face_diagnostics_started")
        var cameraProvider: ProcessCameraProvider? = null
        val startCamera = Runnable {
            try {
                cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                analysis.setAnalyzer(
                    analysisExecutor,
                    OpenCvFaceAnalyzer(
                        context.applicationContext,
                        mainExecutor,
                        onFaceCount = { faceCount = it },
                        onFrameAnalyzed = { hasAnalyzedFrame = true },
                        onError = { cameraError = it },
                    ),
                )
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    analysis,
                )
                isReady = true
            } catch (error: Exception) {
                cameraError = error.message ?: error.javaClass.simpleName
            }
        }
        cameraProviderFuture.addListener(startCamera, mainExecutor)

        onDispose {
            cameraProvider?.unbindAll()
            analysisExecutor.shutdown()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = onClose) { Text(if (isArabic) "رجوع" else "Retour") }
        }
        Text(if (isArabic) "تشخيص كاميرا الوجه" else "Diagnostic caméra visage", style = MaterialTheme.typography.titleLarge)
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        Text(
            when {
                cameraError.isNotEmpty() -> if (isArabic) "خطأ: $cameraError" else "Erreur : $cameraError"
                !isReady -> if (isArabic) "جارٍ تشغيل الكاميرا والنموذج..." else "Démarrage de la caméra et du modèle…"
                !hasAnalyzedFrame -> if (isArabic) "في انتظار إطار من الكاميرا..." else "En attente des images caméra…"
                isArabic -> "الوجوه المكتشفة: $faceCount"
                else -> "Visages détectés : $faceCount"
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            if (isArabic) "المعالجة محلية. لا يتم حفظ الصور أو تسجيل الحضور." else "Traitement local. Aucune image n’est enregistrée et aucun pointage n’est créé.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private class OpenCvFaceAnalyzer(
    private val context: Context,
    private val mainExecutor: java.util.concurrent.Executor,
    private val onFaceCount: (Int) -> Unit,
    private val onFrameAnalyzed: () -> Unit,
    private val onError: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    private var engine: OpenCvFaceEngine? = null
    private var lastAnalysisAt = 0L
    private var failed = false
    private var loggedFirstFrame = false

    override fun analyze(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (failed || now - lastAnalysisAt < 300) {
            image.close()
            return
        }
        lastAnalysisAt = now

        try {
            val faceEngine = engine ?: OpenCvFaceEngine(context).also { engine = it }
            val bgrFrame = image.toBgrMat()
            val count = try {
                faceEngine.countFaces(bgrFrame)
            } finally {
                bgrFrame.release()
            }
            mainExecutor.execute { onFaceCount(count) }
            mainExecutor.execute(onFrameAnalyzed)
            if (!loggedFirstFrame) {
                KioskDiagnostics.record(context, "face_camera_frame_received", "face_count=$count")
                loggedFirstFrame = true
            }
        } catch (error: Exception) {
            failed = true
            val message = error.message ?: error.javaClass.simpleName
            KioskDiagnostics.record(context, "face_scan_error", "error_type=${error.javaClass.simpleName}")
            mainExecutor.execute { onError(message) }
        } finally {
            image.close()
        }
    }

    private fun ImageProxy.toBgrMat(): Mat {
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
}