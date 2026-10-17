package com.cappuccino.kiosk

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val brandBackground = Color(0xFF171613)
private val brandPanel = Color(0xFF24221D)
private val brandGold = Color(0xFFD6B57A)
private val brandText = Color(0xFFF6EBD7)
private val brandMuted = Color(0xFFC1B5A3)
private val tunisZone = ZoneId.of("Africa/Tunis")

private fun isPrivateLanIpv4(host: String): Boolean {
    if (host == "localhost" || host == "10.0.2.2") return true

    val octets = host.split(".").map { it.toIntOrNull() ?: return false }
    if (octets.size != 4 || octets.any { it !in 0..255 }) return false

    return octets[0] == 10 ||
        (octets[0] == 172 && octets[1] in 16..31) ||
        (octets[0] == 192 && octets[1] == 168) ||
        (octets[0] == 127 && octets[1] == 0 && octets[2] == 0 && octets[3] == 1)
}

private suspend fun checkServerHealth(context: Context, serverUrl: String): String = withContext(Dispatchers.IO) {
    val startedAt = SystemClock.elapsedRealtime()
    fun finish(status: String, reason: String = ""): String {
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        val details = "status=$status latency_ms=$elapsed ${if (reason.isEmpty()) "" else "reason=$reason"}"
        KioskDiagnostics.record(context, "api_health_check", details)
        return status
    }

    var connection: HttpURLConnection? = null
    try {
        val baseUrl = URL(serverUrl.trim().trimEnd('/'))
        if (baseUrl.protocol !in listOf("http", "https") ||
            (baseUrl.path.isNotEmpty() && baseUrl.path != "/") ||
            baseUrl.userInfo != null || baseUrl.query != null || baseUrl.ref != null
        ) {
            return@withContext finish("invalid", "invalid_server_address")
        }
        if (baseUrl.protocol == "http" && !isPrivateLanIpv4(baseUrl.host)) {
            return@withContext finish("invalid", "http_requires_private_ipv4")
        }

        connection = URL("${baseUrl.toExternalForm().trimEnd('/')}/api/v1/health")
            .openConnection() as HttpURLConnection
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
                    KioskIdleScreen()
                }
            }
        }
    }
}

@Composable
fun KioskIdleScreen() {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("kiosk_settings", Context.MODE_PRIVATE)
    }
    var isArabic by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(ZonedDateTime.now(tunisZone)) }
    var serverUrl by remember { mutableStateOf(preferences.getString("server_url", "") ?: "") }
    var serverUrlToCheck by remember { mutableStateOf(serverUrl) }
    var serverStatus by remember { mutableStateOf(if (serverUrl.isBlank()) "not_paired" else "checking") }
    var isCheckingServer by remember { mutableStateOf(false) }
    var healthCheckRequest by remember { mutableStateOf(0) }
    var diagnosticsCopied by remember { mutableStateOf(false) }
    val locale = if (isArabic) Locale.forLanguageTag("ar") else Locale.FRANCE
    val connectionTitle = when (serverStatus) {
        "ok" -> if (isArabic) "الخادم متاح" else "API accessible"
        "offline" -> if (isArabic) "تعذر الاتصال بالخادم" else "Serveur injoignable"
        "invalid" -> if (isArabic) "عنوان الخادم غير صالح" else "Adresse serveur invalide"
        "checking" -> if (isArabic) "جارٍ التحقق من الاتصال" else "Vérification de la connexion"
        else -> if (isArabic) "الإعداد مطلوب" else "Configuration requise"
    }
    val connectionMessage = when (serverStatus) {
        "ok" -> if (isArabic) {
            "واجهة الخادم متاحة. تسجيل الحضور والتعرف على الموظفين غير مفعّلين بعد. لم يتم التقاط أو حفظ أي بيانات بيومترية."
        } else {
            "L’API est joignable. Le pointage et l’identification des employés ne sont pas encore activés. Aucune image ni donnée biométrique n’est capturée ou enregistrée."
        }
        "offline" -> if (isArabic) {
            "تحقق من تشغيل الخادم واتصال الجهازين بشبكة Wi-Fi الخاصة نفسها. لم يتم تسجيل أي حضور."
        } else {
            "Vérifiez que le serveur fonctionne et que les deux appareils sont sur le même Wi-Fi privé. Aucun pointage n’est enregistré."
        }
        "invalid" -> if (isArabic) {
            "أدخل عنوان IPv4 خاصاً صالحاً للخادم، مثل http://192.168.1.20:8000."
        } else {
            "Saisissez une adresse IPv4 privée valide, par exemple http://192.168.1.20:8000."
        }
        else -> if (isArabic) {
            "أدخل عنوان الخادم واحفظه لاختبار الاتصال. تسجيل الحضور والتعرف على الوجه غير مفعّلين."
        } else {
            "Saisissez l’adresse du serveur et enregistrez-la pour tester la connexion. Le pointage et la reconnaissance faciale restent désactivés."
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = ZonedDateTime.now(tunisZone)
            delay(1_000)
        }
    }

    LaunchedEffect(serverUrlToCheck, healthCheckRequest) {
        if (serverUrlToCheck.isBlank()) {
            serverStatus = "not_paired"
            return@LaunchedEffect
        }

        while (true) {
            isCheckingServer = true
            if (serverStatus == "not_paired") serverStatus = "checking"
            serverStatus = checkServerHealth(context, serverUrlToCheck)
            isCheckingServer = false
            delay(30_000)
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_cappuccino),
                contentDescription = if (isArabic) "شعار كابوتشينو لاك 1" else "Logo Cappuccino Lac 1",
                modifier = Modifier
                    .size(76.dp)
                    .padding(8.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Cappuccino Lac 1",
                color = brandText,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (isArabic) "نظام حضور الموظفين" else "Pointage du personnel",
                color = brandGold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = currentTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)),
                color = brandText,
                fontSize = 36.sp,
                fontWeight = FontWeight.Light,
            )
            Text(
                text = currentTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)),
                color = brandMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = brandPanel),
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF49391F),
                        modifier = Modifier.size(52.dp),
                    ) {
                        Text(
                            text = "!",
                            color = brandGold,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = connectionTitle,
                        color = brandText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = connectionMessage,
                        color = brandMuted,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(18.dp))
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(if (isArabic) "عنوان الخادم" else "Adresse du serveur") },
                        placeholder = { Text("http://192.168.1.20:8000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        enabled = !isCheckingServer,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            preferences.edit().putString("server_url", serverUrl.trim()).apply()
                            KioskDiagnostics.record(context, "server_address_saved")
                            diagnosticsCopied = false
                            serverUrlToCheck = serverUrl.trim()
                            healthCheckRequest += 1
                        },
                        enabled = serverUrl.isNotBlank() && !isCheckingServer,
                    ) {
                        Text(
                            when {
                                isArabic && isCheckingServer -> "جارٍ الاتصال..."
                                isArabic -> "حفظ واختبار الاتصال"
                                isCheckingServer -> "Vérification..."
                                else -> "Enregistrer et tester"
                            }
                        )
                    }
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("Cappuccino kiosk diagnostics", KioskDiagnostics.read(context))
                            )
                            diagnosticsCopied = true
                            KioskDiagnostics.record(context, "diagnostics_copied")
                        },
                    ) {
                        Text(
                            if (isArabic) {
                                if (diagnosticsCopied) "تم نسخ السجلات" else "نسخ سجلات التشخيص"
                            } else {
                                if (diagnosticsCopied) "Journaux copiés" else "Copier les journaux"
                            }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isCheckingServer) {
                                if (isArabic) "جارٍ الاتصال..." else "Vérification..."
                            } else if (isArabic) {
                                when (serverStatus) {
                                    "ok" -> "الخادم: متاح"
                                    "checking" -> "الخادم: جارٍ الاتصال..."
                                    "invalid" -> "الخادم: عنوان غير صالح"
                                    "offline" -> "الخادم: غير متصل"
                                    else -> "الخادم: غير مقترن"
                                }
                            } else {
                                when (serverStatus) {
                                    "ok" -> "API : accessible"
                                    "checking" -> "Serveur : connexion..."
                                    "invalid" -> "Serveur : adresse invalide"
                                    "offline" -> "Serveur : hors ligne"
                                    else -> "Serveur : non jumelé"
                                }
                            },
                            color = if (serverStatus == "ok") Color(0xFF8BCB9B) else brandMuted,
                            fontSize = 12.sp,
                        )
                        Text(
                            text = if (isArabic) "الوجه: غير مفعّل" else "Visage : désactivé",
                            color = brandGold,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Button(onClick = { isArabic = !isArabic }) {
                Text(if (isArabic) "Français" else "العربية")
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (isArabic) "Zwanski Vulzero · يعمل محلياً" else "Zwanski Vulzero · traitement local",
                color = brandMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
