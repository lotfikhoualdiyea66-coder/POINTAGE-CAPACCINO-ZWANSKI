package com.cappuccino.kiosk

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID

private val brandBackground = Color(0xFF171613)
private val brandPanel = Color(0xFF24221D)
private val brandGold = Color(0xFFD6B57A)
private val brandText = Color(0xFFF6EBD7)
private val brandMuted = Color(0xFFC1B5A3)
private val brandSuccess = Color(0xFF8BCB9B)
private val brandDanger = Color(0xFFE49A8B)
private val tunisZone = ZoneId.of("Africa/Tunis")

private const val PREF_SERVER_URL = "server_url"
private const val PREF_KIOSK_TOKEN = "kiosk_token"

private fun isPrivateLanIpv4(host: String): Boolean {
    if (host == "localhost" || host == "10.0.2.2") return true

    val octets = host.split(".").map { it.toIntOrNull() ?: return false }
    if (octets.size != 4 || octets.any { it !in 0..255 }) return false

    return octets[0] == 10 ||
        (octets[0] == 172 && octets[1] in 16..31) ||
        (octets[0] == 192 && octets[1] == 168) ||
        (octets[0] == 127 && octets[1] == 0 && octets[2] == 0 && octets[3] == 1)
}

/** Returns the server base URL without a trailing slash, or null when it is not an allowed LAN address. */
private fun normalizeServerUrl(raw: String): String? = try {
    val baseUrl = URL(raw.trim().trimEnd('/'))
    when {
        baseUrl.protocol !in listOf("http", "https") -> null
        baseUrl.path.isNotEmpty() && baseUrl.path != "/" -> null
        baseUrl.userInfo != null || baseUrl.query != null || baseUrl.ref != null -> null
        baseUrl.protocol == "http" && !isPrivateLanIpv4(baseUrl.host) -> null
        else -> baseUrl.toExternalForm().trimEnd('/')
    }
} catch (error: Exception) {
    null
}

private suspend fun checkServerHealth(context: Context, serverUrl: String): String = withContext(Dispatchers.IO) {
    val startedAt = SystemClock.elapsedRealtime()
    fun finish(status: String, reason: String = ""): String {
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        val details = "status=$status latency_ms=$elapsed ${if (reason.isEmpty()) "" else "reason=$reason"}"
        KioskDiagnostics.record(context, "api_health_check", details)
        return status
    }

    val baseUrl = normalizeServerUrl(serverUrl) ?: return@withContext finish("invalid", "invalid_server_address")
    var connection: HttpURLConnection? = null
    try {
        connection = URL("$baseUrl/api/v1/health").openConnection() as HttpURLConnection
        connection.connectTimeout = 3_000
        connection.readTimeout = 3_000
        connection.requestMethod = "GET"

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            return@withContext finish("offline", "http_${connection.responseCode}")
        }
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        if (JSONObject(response).optString("status") == "ok") finish("ok") else finish("offline", "unexpected_health_response")
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        finish("offline", error.javaClass.simpleName)
    } finally {
        connection?.disconnect()
    }
}

private fun tr(isArabic: Boolean, french: String, arabic: String) = if (isArabic) arabic else french

private fun actionLabel(isArabic: Boolean, action: String) = when (action) {
    "CLOCK_IN" -> tr(isArabic, "Arrivée", "تسجيل الدخول")
    "CLOCK_OUT" -> tr(isArabic, "Départ", "تسجيل الخروج")
    "BREAK_START" -> tr(isArabic, "Début de pause", "بداية الاستراحة")
    "BREAK_END" -> tr(isArabic, "Fin de pause", "نهاية الاستراحة")
    else -> action
}

private fun stateLabel(isArabic: Boolean, state: String) = when (state) {
    "clocked_in" -> tr(isArabic, "En service", "في العمل")
    "on_break" -> tr(isArabic, "En pause", "في استراحة")
    else -> tr(isArabic, "Hors service", "خارج الدوام")
}

private fun actionColor(action: String) = when (action) {
    "CLOCK_IN" -> Color(0xFF7FBF8E)
    "CLOCK_OUT" -> Color(0xFFE0907E)
    else -> brandGold
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KioskDiagnostics.record(applicationContext, "kiosk_started")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = brandGold,
                    onPrimary = brandBackground,
                    background = brandBackground,
                    surface = brandPanel,
                    onSurface = brandText,
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = brandBackground,
                ) {
                    KioskApp()
                }
            }
        }
    }
}

@Composable
fun KioskApp() {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("kiosk_settings", Context.MODE_PRIVATE)
    }
    var isArabic by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf(preferences.getString(PREF_SERVER_URL, "") ?: "") }
    var kioskToken by remember { mutableStateOf(preferences.getString(PREF_KIOSK_TOKEN, "") ?: "") }
    var showSetup by remember { mutableStateOf(kioskToken.isBlank()) }
    var showFaceDiagnostics by remember { mutableStateOf(false) }

    if (showFaceDiagnostics) {
        FaceCameraDiagnosticsScreen(isArabic = isArabic, onClose = { showFaceDiagnostics = false })
        return
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            KioskHeader(isArabic)
            Spacer(Modifier.height(24.dp))
            if (showSetup || kioskToken.isBlank()) {
                SetupCard(
                    isArabic = isArabic,
                    initialServerUrl = serverUrl,
                    canCancel = kioskToken.isNotBlank(),
                    onPaired = { pairedUrl, token ->
                        preferences.edit()
                            .putString(PREF_SERVER_URL, pairedUrl)
                            .putString(PREF_KIOSK_TOKEN, token)
                            .apply()
                        serverUrl = pairedUrl
                        kioskToken = token
                        showSetup = false
                    },
                    onCancel = { showSetup = false },
                    onOpenFaceTest = { showFaceDiagnostics = true },
                )
            } else {
                ClockCard(
                    isArabic = isArabic,
                    serverUrl = serverUrl,
                    kioskToken = kioskToken,
                    onUnpaired = {
                        KioskDiagnostics.record(context, "kiosk_unpaired_by_server")
                        preferences.edit().remove(PREF_KIOSK_TOKEN).apply()
                        kioskToken = ""
                        showSetup = true
                    },
                )
            }
            Spacer(Modifier.height(22.dp))
            Button(onClick = { isArabic = !isArabic }) {
                Text(if (isArabic) "Français" else "العربية")
            }
            if (!showSetup && kioskToken.isNotBlank()) {
                TextButton(onClick = { showSetup = true }) {
                    Text(tr(isArabic, "Configuration de la borne", "إعدادات الجهاز"), color = brandMuted, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = tr(isArabic, "Zwanski Vulzero · traitement local", "Zwanski Vulzero · يعمل محلياً"),
                color = brandMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun KioskHeader(isArabic: Boolean) {
    var currentTime by remember { mutableStateOf(ZonedDateTime.now(tunisZone)) }
    val locale = if (isArabic) Locale.forLanguageTag("ar") else Locale.FRANCE
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = ZonedDateTime.now(tunisZone)
            delay(1_000)
        }
    }
    Image(
        painter = painterResource(R.drawable.ic_cappuccino),
        contentDescription = tr(isArabic, "Logo Cappuccino Lac 1", "شعار كابوتشينو لاك 1"),
        modifier = Modifier
            .size(76.dp)
            .padding(8.dp),
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = "Cappuccino Lac 1",
        color = brandText,
        fontSize = 26.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
    Text(
        text = tr(isArabic, "Pointage du personnel", "نظام حضور الموظفين"),
        color = brandGold,
        fontSize = 15.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = currentTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)),
        color = brandText,
        fontSize = 40.sp,
        fontWeight = FontWeight.Light,
    )
    Text(
        text = currentTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)),
        color = brandMuted,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun KioskCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = brandPanel),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
private fun CardTitle(title: String, message: String? = null) {
    Text(
        text = title,
        color = brandText,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
    if (message != null) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = brandMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun SetupCard(
    isArabic: Boolean,
    initialServerUrl: String,
    canCancel: Boolean,
    onPaired: (serverUrl: String, token: String) -> Unit,
    onCancel: () -> Unit,
    onOpenFaceTest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var serverUrl by remember { mutableStateOf(initialServerUrl) }
    var pairingCode by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("${Build.MANUFACTURER} ${Build.MODEL}".trim()) }
    var isPairing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var diagnosticsCopied by remember { mutableStateOf(false) }
    var cameraPermissionDenied by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraPermissionDenied = !granted
        if (granted) onOpenFaceTest()
    }

    fun pair() {
        val normalized = normalizeServerUrl(serverUrl)
        if (normalized == null) {
            error = tr(
                isArabic,
                "Adresse invalide. Exemple : http://192.168.1.20:8000",
                "عنوان غير صالح. مثال: http://192.168.1.20:8000",
            )
            return
        }
        isPairing = true
        error = null
        scope.launch {
            val result = KioskApi.pair(normalized, pairingCode.trim(), deviceName.trim().ifEmpty { "Kiosk" })
            KioskDiagnostics.record(context, "kiosk_pair_attempt", "result=${result.javaClass.simpleName}")
            isPairing = false
            when (result) {
                is ApiResult.Success -> onPaired(normalized, result.value)
                ApiResult.InvalidCredentials -> error = tr(isArabic, "Code de jumelage incorrect.", "رمز الاقتران غير صحيح.")
                ApiResult.Locked -> error = tr(
                    isArabic,
                    "Trop d’essais. Réessayez dans 15 minutes.",
                    "محاولات كثيرة. أعد المحاولة بعد 15 دقيقة.",
                )
                else -> error = tr(
                    isArabic,
                    "Serveur injoignable. Vérifiez l’adresse et que le téléphone est sur le même Wi-Fi.",
                    "تعذر الاتصال بالخادم. تحقق من العنوان ومن اتصال الهاتف بنفس شبكة Wi-Fi.",
                )
            }
        }
    }

    KioskCard {
        CardTitle(
            title = tr(isArabic, "Jumeler la borne", "اقتران الجهاز"),
            message = tr(
                isArabic,
                "Saisissez l’adresse du serveur et le code de jumelage fourni par l’administrateur.",
                "أدخل عنوان الخادم ورمز الاقتران الذي قدّمه المسؤول.",
            ),
        )
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(tr(isArabic, "Adresse du serveur", "عنوان الخادم")) },
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
            placeholder = { Text("http://192.168.1.20:8000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            enabled = !isPairing,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = pairingCode,
            onValueChange = { pairingCode = it.filter(Char::isLetterOrDigit).take(64) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(tr(isArabic, "Code de jumelage", "رمز الاقتران")) },
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
            enabled = !isPairing,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = deviceName,
            onValueChange = { deviceName = it.take(255) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(tr(isArabic, "Nom de la borne", "اسم الجهاز")) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (!isPairing) pair() }),
            enabled = !isPairing,
        )
        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = brandDanger, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { pair() },
            enabled = serverUrl.isNotBlank() && pairingCode.length >= 4 && !isPairing,
        ) {
            Text(if (isPairing) tr(isArabic, "Jumelage…", "جارٍ الاقتران…") else tr(isArabic, "Jumeler", "اقتران"))
        }
        if (canCancel) {
            TextButton(onClick = onCancel) { Text(tr(isArabic, "Annuler", "إلغاء")) }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Cappuccino kiosk diagnostics", KioskDiagnostics.read(context)))
                    diagnosticsCopied = true
                    KioskDiagnostics.record(context, "diagnostics_copied")
                },
            ) {
                Text(
                    if (diagnosticsCopied) tr(isArabic, "Journaux copiés", "تم نسخ السجلات")
                    else tr(isArabic, "Copier les journaux", "نسخ السجلات"),
                    fontSize = 13.sp,
                )
            }
            TextButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        onOpenFaceTest()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
            ) {
                Text(tr(isArabic, "Tester la caméra", "اختبار الكاميرا"), fontSize = 13.sp)
            }
        }
        if (cameraPermissionDenied) {
            Text(tr(isArabic, "Autorisation caméra refusée", "إذن الكاميرا مرفوض"), color = brandMuted, fontSize = 12.sp)
        }
    }
}

private sealed class ClockStep {
    data object Entry : ClockStep()
    data class Choose(val status: EmployeeStatus, val code: String, val pin: String) : ClockStep()
    data class Done(val result: ClockResult) : ClockStep()
}

@Composable
private fun ClockCard(
    isArabic: Boolean,
    serverUrl: String,
    kioskToken: String,
    onUnpaired: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var serverStatus by remember { mutableStateOf("checking") }
    var step by remember { mutableStateOf<ClockStep>(ClockStep.Entry) }
    var employeeCode by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Reused when a tap is retried after a network error, so the server never records it twice.
    var pendingKey by remember { mutableStateOf<Pair<String, String>?>(null) }

    fun reset() {
        step = ClockStep.Entry
        employeeCode = ""
        pin = ""
        error = null
        pendingKey = null
    }

    fun errorFor(result: ApiResult<*>): String? = when (result) {
        is ApiResult.Success -> null
        ApiResult.InvalidCredentials -> tr(isArabic, "Code employé ou PIN incorrect.", "رمز الموظف أو الرمز السري غير صحيح.")
        ApiResult.Locked -> tr(
            isArabic,
            "Trop d’essais incorrects. Réessayez dans 5 minutes.",
            "محاولات خاطئة كثيرة. أعد المحاولة بعد 5 دقائق.",
        )
        ApiResult.InvalidAction -> tr(
            isArabic,
            "Action déjà enregistrée ou impossible maintenant.",
            "تم تسجيل هذه الحركة مسبقاً أو أنها غير ممكنة الآن.",
        )
        ApiResult.Offline -> tr(
            isArabic,
            "Serveur injoignable. Réessayez ; rien n’a été enregistré si l’erreur persiste.",
            "تعذر الاتصال بالخادم. أعد المحاولة.",
        )
        ApiResult.KioskUnpaired -> null
    }

    LaunchedEffect(serverUrl) {
        while (true) {
            serverStatus = checkServerHealth(context, serverUrl)
            delay(if (serverStatus == "ok") 30_000 else 10_000)
        }
    }

    LaunchedEffect(step) {
        when (step) {
            is ClockStep.Choose -> {
                delay(30_000)
                reset()
            }
            is ClockStep.Done -> {
                delay(6_000)
                reset()
            }
            ClockStep.Entry -> Unit
        }
    }

    fun identify() {
        isBusy = true
        error = null
        scope.launch {
            val code = employeeCode.trim().uppercase(Locale.ROOT)
            val result = KioskApi.identify(serverUrl, kioskToken, code, pin)
            isBusy = false
            when (result) {
                is ApiResult.Success -> step = ClockStep.Choose(result.value, code, pin)
                ApiResult.KioskUnpaired -> onUnpaired()
                else -> {
                    error = errorFor(result)
                    pin = ""
                }
            }
        }
    }

    fun clock(choice: ClockStep.Choose, action: String) {
        isBusy = true
        error = null
        val key = pendingKey?.takeIf { it.first == action }?.second ?: UUID.randomUUID().toString()
        pendingKey = action to key
        scope.launch {
            val result = KioskApi.clock(serverUrl, kioskToken, choice.code, choice.pin, action, key)
            KioskDiagnostics.record(context, "kiosk_clock", "action=$action result=${result.javaClass.simpleName}")
            isBusy = false
            when (result) {
                is ApiResult.Success -> {
                    pendingKey = null
                    step = ClockStep.Done(result.value)
                }
                ApiResult.KioskUnpaired -> onUnpaired()
                else -> error = errorFor(result)
            }
        }
    }

    KioskCard {
        when (val current = step) {
            ClockStep.Entry -> {
                CardTitle(
                    title = tr(isArabic, "Pointer", "تسجيل الحضور"),
                    message = tr(isArabic, "Saisissez votre code employé et votre PIN.", "أدخل رمز الموظف والرمز السري."),
                )
                OutlinedTextField(
                    value = employeeCode,
                    onValueChange = { employeeCode = it.take(64) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(tr(isArabic, "Code employé", "رمز الموظف")) },
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next,
                    ),
                    enabled = !isBusy,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(tr(isArabic, "PIN", "الرمز السري")) },
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (employeeCode.length >= 2 && pin.length >= 4 && !isBusy) identify()
                    }),
                    enabled = !isBusy,
                )
                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = brandDanger, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { identify() },
                    enabled = employeeCode.trim().length >= 2 && pin.length >= 4 && !isBusy,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) {
                    Text(
                        if (isBusy) tr(isArabic, "Vérification…", "جارٍ التحقق…") else tr(isArabic, "Continuer", "متابعة"),
                        fontSize = 17.sp,
                    )
                }
            }

            is ClockStep.Choose -> {
                CardTitle(
                    title = tr(isArabic, "Bonjour ${current.status.fullName}", "مرحباً ${current.status.fullName}"),
                    message = "${tr(isArabic, "Statut", "الحالة")} : ${stateLabel(isArabic, current.status.state)}",
                )
                current.status.allowedActions.forEach { action ->
                    Button(
                        onClick = { clock(current, action) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = actionColor(action), contentColor = brandBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(vertical = 4.dp),
                    ) {
                        Text(actionLabel(isArabic, action), fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = brandDanger, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { reset() }, enabled = !isBusy) {
                    Text(tr(isArabic, "Annuler", "إلغاء"))
                }
            }

            is ClockStep.Done -> {
                val time = runCatching {
                    LocalDateTime.parse(current.result.eventTime).format(DateTimeFormatter.ofPattern("HH:mm"))
                }.getOrDefault("")
                Text("✓", color = brandSuccess, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                CardTitle(
                    title = tr(isArabic, "Pointage enregistré", "تم التسجيل بنجاح"),
                    message = "${actionLabel(isArabic, current.result.eventType)} · $time\n" +
                        "${current.result.status.fullName}\n" +
                        "${tr(isArabic, "Statut", "الحالة")} : ${stateLabel(isArabic, current.result.status.state)}",
                )
                Button(onClick = { reset() }) { Text(tr(isArabic, "Terminer", "إنهاء")) }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = when (serverStatus) {
                "ok" -> tr(isArabic, "● Serveur connecté", "● الخادم متصل")
                "checking" -> tr(isArabic, "Connexion…", "جارٍ الاتصال…")
                else -> tr(isArabic, "● Serveur hors ligne", "● الخادم غير متصل")
            },
            color = when (serverStatus) {
                "ok" -> brandSuccess
                "checking" -> brandMuted
                else -> brandDanger
            },
            fontSize = 12.sp,
        )
    }
}
