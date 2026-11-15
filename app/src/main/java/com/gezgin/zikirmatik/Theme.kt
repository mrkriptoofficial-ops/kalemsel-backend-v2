package com.gezgin.zikirmatik

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ───────────── Yazı aileleri (SIL OFL) ─────────────
val Display = FontFamily(
    Font(R.font.cormorant_light, FontWeight.Light),
    Font(R.font.cormorant_regular, FontWeight.Normal),
    Font(R.font.cormorant_medium, FontWeight.Medium),
    Font(R.font.cormorant_semibold, FontWeight.SemiBold),
)
val Sans = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)
val Arabic = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold),
)

const val NUM_FEATURES = "lnum, tnum"

// ───────────── Palet ─────────────
class Pal(
    val name: String,
    val dark: Boolean,
    val bgTop: Color, val bgMid: Color, val bgBottom: Color,
    val text: Color, val sub: Color,
    val gold: Color, val goldLight: Color, val goldDeep: Color,
    val goldText: Color,        // altının yazı olarak kullanıldığı yerler (kontrast güvenli)
    val onGold: Color,
    val glassTop: Color, val glassBottom: Color, val glassStroke: Color,
    val dome0: Color, val dome1: Color, val dome2: Color, val domeGlow: Color,
    val beadMid: Color, val beadEdge: Color,
    val sheet: Color, val sheetLine: Color,
    val glowA: Color, val glowB: Color, val glowC: Color,
    val stars: Float,
    numTop: Color, numMid: Color, numBottom: Color,
) {
    val goldBrush: Brush = Brush.linearGradient(listOf(goldDeep, gold, goldLight, gold, goldDeep))
    val numberBrush: Brush = Brush.verticalGradient(listOf(numTop, numMid, numBottom))
    val faint: Color = sub.copy(alpha = 0.35f)
    val hair: Color = if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f)
}

val SAFIR = Pal(
    name = "Safir", dark = true,
    bgTop = Color(0xFF14284F), bgMid = Color(0xFF0B1733), bgBottom = Color(0xFF040917),
    text = Color(0xFFF6F0E1), sub = Color(0xFFA9B4CB),
    gold = Color(0xFFD4AE62), goldLight = Color(0xFFF1D9A0), goldDeep = Color(0xFF9C7838),
    goldText = Color(0xFFEBCB86), onGold = Color(0xFF071028),
    glassTop = Color.White.copy(alpha = 0.085f), glassBottom = Color.White.copy(alpha = 0.025f), glassStroke = Color.White.copy(alpha = 0.26f),
    dome0 = Color(0xFF2A4987), dome1 = Color(0xFF142852), dome2 = Color(0xFF081430), domeGlow = Color(0xFF4A78C8),
    beadMid = Color(0xFFA9C2EE), beadEdge = Color(0xFF2B4A8A),
    sheet = Color(0xFF101F42), sheetLine = Color(0xFFD4AE62).copy(alpha = 0.35f),
    glowA = Color(0xFF2E5299).copy(alpha = 0.55f), glowB = Color(0xFF41347A).copy(alpha = 0.30f), glowC = Color(0xFF1C3D78).copy(alpha = 0.40f),
    stars = 1f,
    numTop = Color(0xFFFFF9EA), numMid = Color(0xFFF1D9A0), numBottom = Color(0xFFD4AE62),
)

val GECE = Pal(
    name = "Gece", dark = true,
    bgTop = Color(0xFF07070A), bgMid = Color(0xFF000000), bgBottom = Color(0xFF000000),
    text = Color(0xFFF4EEE1), sub = Color(0xFFABA59B),
    gold = Color(0xFFD6B068), goldLight = Color(0xFFF2DBA5), goldDeep = Color(0xFF9A773A),
    goldText = Color(0xFFEDCD8A), onGold = Color(0xFF0A0804),
    glassTop = Color.White.copy(alpha = 0.07f), glassBottom = Color.White.copy(alpha = 0.015f), glassStroke = Color.White.copy(alpha = 0.20f),
    dome0 = Color(0xFF26262E), dome1 = Color(0xFF111116), dome2 = Color(0xFF040405), domeGlow = Color(0xFF8A7444),
    beadMid = Color(0xFFD9CFBC), beadEdge = Color(0xFF4A4335),
    sheet = Color(0xFF0E0E11), sheetLine = Color(0xFFD6B068).copy(alpha = 0.30f),
    glowA = Color(0xFFD6B068).copy(alpha = 0.06f), glowB = Color(0xFF2A2440).copy(alpha = 0.22f), glowC = Color(0xFF10223F).copy(alpha = 0.22f),
    stars = 0.5f,
    numTop = Color(0xFFFFF8E6), numMid = Color(0xFFF2DBA5), numBottom = Color(0xFFD6B068),
)

val FILDISI = Pal(
    name = "Fildişi", dark = false,
    bgTop = Color(0xFFFCF9F3), bgMid = Color(0xFFF5EEE1), bgBottom = Color(0xFFECE2CF),
    text = Color(0xFF1E1912), sub = Color(0xFF5A5142),
    gold = Color(0xFFB8913F), goldLight = Color(0xFFE2C47E), goldDeep = Color(0xFF7A5A1E),
    goldText = Color(0xFF765519), onGold = Color(0xFF1E1912),
    glassTop = Color.White.copy(alpha = 0.72f), glassBottom = Color.White.copy(alpha = 0.38f), glassStroke = Color.White,
    dome0 = Color(0xFFFFFFFF), dome1 = Color(0xFFF5EFE3), dome2 = Color(0xFFE4D8C1), domeGlow = Color(0xFFD9B86A),
    beadMid = Color(0xFFF1E7D4), beadEdge = Color(0xFFB09A70),
    sheet = Color(0xFFFBF7EF), sheetLine = Color(0xFFB8913F).copy(alpha = 0.40f),
    glowA = Color(0xFFF3DDAA).copy(alpha = 0.55f), glowB = Color(0xFFE6DCF5).copy(alpha = 0.45f), glowC = Color(0xFFDCE8F6).copy(alpha = 0.50f),
    stars = 0f,
    numTop = Color(0xFFA27E36), numMid = Color(0xFF7A5A1E), numBottom = Color(0xFF5C4212),
)

val PALETLER = listOf(SAFIR, GECE, FILDISI)

val LocalPal = staticCompositionLocalOf { SAFIR }

// ───────────── Hazır yazı stilleri ─────────────
fun labelStyle(c: Color) = TextStyle(
    fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 2.4.sp, color = c,
)

fun bodyStyle(c: Color) = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = c)

fun titleStyle(b: Brush) = TextStyle(
    fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = 0.5.sp, brush = b,
)
