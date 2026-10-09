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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isArabic) "الخادم: غير مقترن" else "Serveur : non jumelé",
                            color = brandMuted,
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
