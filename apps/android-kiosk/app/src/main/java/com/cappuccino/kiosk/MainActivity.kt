package com.cappuccino.kiosk

import android.os.Bundle
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

private suspend fun checkServerHealth(serverUrl: String): String = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        val baseUrl = URL(serverUrl.trim().trimEnd('/'))
        if (baseUrl.protocol !in listOf("http", "https") || (baseUrl.path.isNotEmpty() && baseUrl.path != "/")) {
            return@withContext "invalid"
        }
        if (baseUrl.protocol == "http" && !isPrivateLanIpv4(baseUrl.host)) {
            return@withContext "invalid"
        }

        connection = URL("${baseUrl.toExternalForm().trimEnd('/')}/api/v1/health")
            .openConnection() as HttpURLConnection
        connection.connectTimeout = 3_000
        connection.readTimeout = 3_000
        connection.requestMethod = "GET"

        if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext "offline"
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        if (JSONObject(response).optString("status") == "ok") "ok" else "offline"
    } catch (_: Exception) {
        "offline"
    } finally {
        connection?.disconnect()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    var isArabic by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(ZonedDateTime.now(tunisZone)) }
    var serverUrl by remember { mutableStateOf("") }
    var serverStatus by remember { mutableStateOf("not_paired") }
    var isCheckingServer by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val locale = if (isArabic) Locale.forLanguageTag("ar") else Locale.FRANCE

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = ZonedDateTime.now(tunisZone)
            delay(1_000)
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
                        text = if (isArabic) "وضع الإعداد مطلوب" else "Configuration requise",
                        color = brandText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isArabic) {
                            "الخادم غير مقترن والتعرّف على الوجه غير مفعّل. لم يتم التقاط أو حفظ أي صورة. يجب إكمال الإعداد والتحقق من الترخيص قبل تسجيل الحضور."
                        } else {
                            "Le serveur n’est pas jumelé et la reconnaissance faciale n’est pas activée. Aucune image n’est capturée ni enregistrée. Terminez le jumelage et vérifiez les autorisations avant d’activer le pointage."
                        },
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
                            coroutineScope.launch {
                                isCheckingServer = true
                                serverStatus = "checking"
                                serverStatus = checkServerHealth(serverUrl)
                                isCheckingServer = false
                            }
                        },
                        enabled = serverUrl.isNotBlank() && !isCheckingServer,
                    ) {
                        Text(
                            when {
                                isArabic && isCheckingServer -> "جارٍ الاتصال..."
                                isArabic -> "اختبار الاتصال"
                                isCheckingServer -> "Connexion..."
                                else -> "Tester la connexion"
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
                            text = if (isArabic) {
                                when (serverStatus) {
                                    "ok" -> "الخادم: متصل"
                                    "checking" -> "الخادم: جارٍ الاتصال..."
                                    "invalid" -> "الخادم: عنوان غير صالح"
                                    "offline" -> "الخادم: غير متصل"
                                    else -> "الخادم: غير مقترن"
                                }
                            } else {
                                when (serverStatus) {
                                    "ok" -> "Serveur : connecté"
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
