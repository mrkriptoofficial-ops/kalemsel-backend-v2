package com.gezgin.zikirmatik

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import kotlin.math.exp
import kotlin.math.sqrt
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val TR = Locale.forLanguageTag("tr")
fun fmt(n: Int): String = String.format(I18n.numLocale, "%,d", n)

@Composable
fun CounterScreen(st: AppState, go: (Scr) -> Unit, sheet: (SheetKind) -> Unit) {
    val p = LocalPal.current
    val d = st.display()
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Üst çubuk
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            GlyphButton(G.LIST, L("Zikirlerim", "My Dhikr")) { go(Scr.ZIKIRLER) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    L("ZİKİRMATİK", "ZIKIRMATIK"),
                    style = TextStyle(brush = p.goldBrush, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, letterSpacing = 7.sp),
                    modifier = Modifier.padding(start = 7.dp),
                )
                Spacer(Modifier.height(4.dp))
                Ornament(88.dp)
            }
            GlyphButton(G.GRID, L("Menü", "Menu")) { sheet(SheetKind.Menu) }
        }
        Spacer(Modifier.height(16.dp))

        ZikirCard(st, d)

        // Dokunma alanı: kubbe ve çevresindeki tüm boşluk sayar
        var pressed by remember { mutableStateOf(false) }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .semantics {
                    contentDescription = Lf("{0}. {1}, hedef {2}. Saymak için dokunun.", "{0}. {1} of {2}. Tap to count.", d.okunus, d.shown, d.target)
                    role = Role.Button
                    liveRegion = LiveRegionMode.Polite
                    onClick { st.tap(); true }
                }
                // Parmak değdiği an sayar (bırakmayı beklemez): gecikmesiz his
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        pressed = true
                        st.tap()
                        waitForUpOrCancellation()
                        pressed = false
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Dome(d, st.settings.bead, pressed, st.pulseKey, st.celebrateKey, st.shakeKey, Modifier.fillMaxSize().padding(vertical = 4.dp))
        }

        ControlRow(st, d, sheet)
        Spacer(Modifier.height(16.dp))
        StatStrip(st)
        Spacer(Modifier.height(10.dp))
    }
}

// ───────────── Zikir kartı ─────────────
@Composable
private fun ZikirCard(st: AppState, d: Disp) {
    val p = LocalPal.current
    val n = st.zikirs.size
    var drag by remember { mutableFloatStateOf(0f) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier
                .fillMaxWidth()
                .glass(p, RoundedCornerShape(28.dp))
                .pointerInput(d.vird == null, n) {
                    if (d.vird == null && n > 1) {
                        detectHorizontalDragGestures(
                            onDragStart = { drag = 0f },
                            onDragEnd = {
                                val dir = if (I18n.rtl) -1 else 1   // sağdan sola dillerde kaydırma yönü ters
                                if (drag < -90f) st.step(dir) else if (drag > 90f) st.step(-dir)
                                drag = 0f
                            },
                        ) { _, dx -> drag += dx }
                    }
                }
                .padding(top = 14.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (d.vird != null) VirdHeader(st, d)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (d.vird == null && n > 1) GlyphButton(G.LEFT, L("Önceki zikir", "Previous dhikr"), 40.dp, bordered = false) { st.step(-1) } else Spacer(Modifier.width(12.dp))
                AnimatedContent(
                    targetState = d,
                    contentKey = { it.key },
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val dir = st.cardDir
                        (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { it / 5 * dir } + fadeIn(tween(380))) togetherWith
                            (slideOutHorizontally(tween(260)) { -it / 5 * dir } + fadeOut(tween(200))) using SizeTransform(clip = false)
                    },
                    label = "kart",
                ) { z ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (z.arapca.isNotBlank()) {
                            Text(
                                z.arapca,
                                style = TextStyle(
                                    fontFamily = Arabic,
                                    fontSize = when { z.arapca.length > 150 -> 16.sp; z.arapca.length > 70 -> 20.sp; z.arapca.length > 40 -> 24.sp; else -> 31.sp },
                                    lineHeight = when { z.arapca.length > 150 -> 29.sp; z.arapca.length > 70 -> 35.sp; z.arapca.length > 40 -> 42.sp; else -> 54.sp },
                                    color = p.text, textAlign = TextAlign.Center, textDirection = TextDirection.Rtl,
                                ),
                                maxLines = 7,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                z.okunus.uppercase(TR),
                                style = labelStyle(p.goldText).copy(fontSize = 12.sp, letterSpacing = 2.6.sp, textAlign = TextAlign.Center),
                                maxLines = 2,
                            )
                        } else {
                            Text(
                                z.okunus,
                                style = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 34.sp, color = p.text, textAlign = TextAlign.Center),
                                maxLines = 2,
                            )
                        }
                        // Anlam ve fazilet metinleri Türkçe; diğer dillerde sayaç kartında gösterilmez (Kütüphane'de görünür)
                        if (z.anlam.isNotBlank() && I18n.tr) {
                            Spacer(Modifier.height(5.dp))
                            Text(z.anlam, style = bodyStyle(p.sub).copy(fontSize = 13.sp, textAlign = TextAlign.Center), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        if (z.fazilet.isNotBlank() && I18n.tr) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                z.fazilet,
                                style = TextStyle(fontFamily = Sans, fontSize = 13.5.sp, lineHeight = 19.sp, color = p.sub, textAlign = TextAlign.Center),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (d.vird == null && n > 1) GlyphButton(G.RIGHT, L("Sonraki zikir", "Next dhikr"), 40.dp, bordered = false) { st.step(1) } else Spacer(Modifier.width(12.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        if (d.vird == null && n in 2..14) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(8.dp)) {
                val ci = st.currentIndex
                st.zikirs.indices.forEach { i ->
                    val w by animateDpAsState(if (i == ci) 18.dp else 5.dp, label = "nokta")
                    Box(
                        Modifier.padding(horizontal = 2.5.dp).width(w).height(5.dp).clip(CircleShape)
                            .background(if (i == ci) p.gold else p.faint)
                    )
                }
            }
        } else if (d.vird == null && n > 14) {
            Text("${st.currentIndex + 1} / $n", style = labelStyle(p.sub), modifier = Modifier.height(14.dp))
        } else Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun VirdHeader(st: AppState, d: Disp) {
    val p = LocalPal.current
    val v = d.vird ?: return
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(v.name.uppercase(TR), style = labelStyle(p.goldText), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                v.steps.forEachIndexed { i, _ ->
                    Box(
                        Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(50))
                            .background(
                                when {
                                    i < d.step -> p.gold
                                    i == d.step -> p.goldLight
                                    else -> p.faint
                                }
                            )
                    )
                }
            }
        }
        Text("  ${d.step + 1}/${v.steps.size}", style = labelStyle(p.sub).copy(fontFeatureSettings = NUM_FEATURES))
        GlyphButton(G.CLOSE, L("Tesbihatı bitir", "End sequence"), 38.dp, bordered = false) { st.endSession() }
    }
}

// ───────────── Kristal kubbe ─────────────
@Composable
private fun Dome(d: Disp, bead: Int, pressed: Boolean, pulseKey: Int, celebrateKey: Int, shakeKey: Int, modifier: Modifier) {
    val p = LocalPal.current
    val shown = d.shown
    val reduce = rememberReducedMotion()
    val sprites = remember { BeadSprites() }

    val prog = remember { Animatable(0f) }
    LaunchedEffect(shown, d.target, d.key) {
        val t = d.progress
        if (t < prog.value - 0.4f) prog.snapTo(0f)
        if (reduce) prog.snapTo(t) else prog.animateTo(t, spring(stiffness = Spring.StiffnessMediumLow))
    }
    // Tespih tanelerinin konumu (tane biriminde); her dokunuşta bir tane kayar
    val rot = remember { Animatable(d.count.toFloat()) }
    LaunchedEffect(d.count, d.key) {
        val t = d.count.toFloat()
        if (reduce || abs(t - rot.value) > 33f || t < rot.value - 0.001f) {
            if (reduce || abs(t - rot.value) > 33f) rot.snapTo(t) else rot.animateTo(t, tween(180))   // geri alma: yumuşak geri dönüş
        } else {
            // Çekiş: tane yerçekimiyle hızlanarak düşer, 2'deki tanelere çarpıp hafifçe seker
            rot.animateTo(t, keyframes {
                durationMillis = PULL_MS + 150
                t at PULL_MS using FastOutLinearInEasing
                (t + 0.07f) at PULL_MS + 55 using LinearOutSlowInEasing
                (t - 0.015f) at PULL_MS + 110
                t at PULL_MS + 150
            })
        }
    }
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(pulseKey) {
        if (pulseKey > 0 && !reduce) {
            // Hızlı sayarken parlamayı baştan başlatıp yanıp sönme etkisi yaratma
            pulse.snapTo(if (pulse.value >= 1f) 0f else minOf(pulse.value, 0.25f))
            pulse.animateTo(1f, tween(480, easing = LinearOutSlowInEasing))
        }
    }
    val burst = remember { Animatable(1f) }
    LaunchedEffect(celebrateKey) { if (celebrateKey > 0) { burst.snapTo(0f); burst.animateTo(1f, tween(1700, easing = LinearOutSlowInEasing)) } }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) { if (shakeKey > 0 && !reduce) for (x in listOf(9f, -7f, 5f, -3f, 0f)) shake.animateTo(x, tween(45)) }
    val press by animateFloatAsState(if (pressed) 1f else 0f, spring(stiffness = 900f), label = "bas")
    // Kehribar koyulaştıkça yazı rengi okunaklı kalacak şekilde döner
    // Koyu malzemede (oltu, kristal) açık renk yazı, açık malzemede koyu yazı
    val darkAmber = domeNeedsLightText(BEADS[bead.coerceIn(0, BEADS.lastIndex)])
    val onAmber by animateColorAsState(if (darkAmber) Color(0xFFFFF4DC) else Color(0xFF2E1703), tween(400), label = "yazı")

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // Kenar boşluklarına taşabilir (requiredSize); yükseklik imame ve püskül için %16 fazla
        val side = minOf(maxWidth + 40.dp, maxHeight / DOME_H, 460.dp)
        val dens = LocalDensity.current
        val numSize = with(dens) { (side * (if (shown >= 1000) 0.18f else 0.25f)).toSp() }
        val lift = side * ((DOME_H - 1f) / 2f)   // kubbe merkezi, kutunun merkezinden bu kadar yukarıda
        val small = with(dens) { (side * 0.032f).toSp() }
        val px = dens.density
        Box(
            Modifier
                .requiredSize(side, side * DOME_H)
                .graphicsLayer {
                    translationX = shake.value * px
                    val sc = 1f - 0.028f * press
                    scaleX = sc; scaleY = sc
                },
            contentAlignment = Alignment.Center,
        ) {
            val mat = BEADS[bead.coerceIn(0, BEADS.lastIndex)]
            Canvas(Modifier.fillMaxSize()) { drawDome(p, mat, sprites, prog.value, rot.value, pulse.value, burst.value, press, reduce, back = true) }
            Canvas(Modifier.fillMaxSize()) { drawDome(p, mat, sprites, prog.value, rot.value, pulse.value, burst.value, press, reduce) }
            // Rakam her zaman tam ortada; diğer yazılar üst üste bindirilir
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = -lift)) {
                AnimatedContent(
                    targetState = shown,
                    transitionSpec = {
                        if (reduce) {
                            fadeIn(tween(90)) togetherWith fadeOut(tween(90))
                        } else {
                            val up = targetState > initialState
                            (slideInVertically(tween(160)) { if (up) it / 5 else -it / 5 } + fadeIn(tween(140))) togetherWith
                                (slideOutVertically(tween(110)) { if (up) -it / 5 else it / 5 } + fadeOut(tween(110))) using SizeTransform(clip = false)
                        }
                    },
                    label = "sayı",
                ) { v ->
                    Text(
                        "$v",
                        style = TextStyle(
                            color = onAmber, fontFamily = Display, fontWeight = FontWeight.Normal,
                            fontSize = numSize, fontFeatureSettings = NUM_FEATURES,
                            shadow = if (darkAmber) Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 3f), 14f) else Shadow(Color(0xFFFFF4D6).copy(alpha = 0.6f), Offset(0f, 2f), 6f),
                        ),
                    )
                }
                Text(
                    Lf("HEDEF  {0}", "TARGET  {0}", fmt(d.target)),
                    style = labelStyle(onAmber.copy(alpha = 0.78f)).copy(fontSize = small, fontFeatureSettings = NUM_FEATURES),
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
            Text(
                L("TAMAMLANDI", "COMPLETE"),
                style = labelStyle(onAmber).copy(fontSize = small, letterSpacing = 3.sp),
                modifier = Modifier.align(Alignment.Center).offset(y = -(side * 0.21f) - lift).padding(start = 3.dp)
                    .graphicsLayer { val b = burst.value; alpha = if (b < 1f) sin(PI.toFloat() * b).coerceIn(0f, 1f) else 0f },
            )
            if (d.laps > 0) {
                Text(
                    Lf("{0}. TUR", "ROUND {0}", d.laps),
                    style = labelStyle(p.onGold).copy(fontWeight = FontWeight.Bold, fontFeatureSettings = NUM_FEATURES),
                    modifier = Modifier.align(Alignment.Center).offset(y = side * 0.19f - lift)
                        .clip(RoundedCornerShape(50)).background(p.goldBrush).padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** Berrak cam kubbe tonları (buz mavisi). */
private val GLASS_TINT = Color(0xFFBFD3FF)
private val GLASS_EDGE = Color(0xFFD6E4FF)

/** Bükümlü ipek tespih ipi renkleri. */
private val CORD_DARK = Color(0xFF5B3518)
private val CORD_LIGHT = Color(0xFFA9773E)
private val CORD_SHINE = Color(0xFFE8C48A).copy(alpha = 0.75f)

/** Çekme izinin rengi (sıcak altın). */
private val PULL_TRAIL = Color(0xFFF6D58C)

/** 2'de biriken tane sayısı ve 1 ile 2 arasındaki ip boşluğu (tane cinsinden). */
/** Tane boyutu çarpanı (1 = aralıklı; ~1.15 = birbirine değen ama üst üste binmeyen taneler). */
private const val BEAD_SCALE = 1.15f
private const val CLUSTER = 3f
private const val PULL_GAP = 1.5f

/** Tanenin 1'den 2'ye düşüş süresi (ms); iniş sesi bu ana denk gelir. */
const val PULL_MS = 170

// Kehribar yansıma renkleri: saf beyaz değil, sıcak krem (beyaz yansıma "sırıtmasın")
private val WARM_SHEEN = Color(0xFFFFE7AE)
private val WARM_HALO = Color(0xFFFFE9BE)
private val WARM_SPEC = Color(0xFFFFF1D2)

/** Tespih tanesi malzemesi. */
class BeadMat(
    val name: String,
    val base: Color, val light: Color, val edge: Color,
    val mottleLight: Color, val mottleDark: Color,
    val spec: Float,
    val silver: Boolean,
    val pattern: BeadPattern = BeadPattern.NONE,
)

/** Doğal taşların yüzey deseni. */
enum class BeadPattern { NONE, DROPS, FLECKS, CHATOYANT, BANDS, VEINS, SHEEN }

val BEADS = listOf(
    // Sıkma kehribar: opak, tereyağı sarısı, yumuşak bulutlanma, gümüş telkari
    BeadMat("Kehribar", Color(0xFFD3922A), Color(0xFFE6AA45), Color(0xFF955A10), Color(0xFFE8B04C), Color(0xFFA0702A), 0.85f, true, BeadPattern.DROPS),
    BeadMat("Oltu", Color(0xFF1C1915), Color(0xFF524940), Color(0xFF040303), Color(0xFF332C25), Color(0xFF0A0908), 1f, true),
    BeadMat("Sedef", Color(0xFFE6DED3), Color(0xFFFFFFFF), Color(0xFFA0928A), Color(0xFFF7E3EE), Color(0xFFD9E5F3), 0.8f, false),
    BeadMat("Kristal", Color(0x66DCE6FF), Color(0xEEFFFFFF), Color(0xCC5E7AB4), Color(0x88FFFFFF), Color(0x55A9BDF0), 1f, false),
    // Akik (karneol): kızıl turuncu, ince katman halkaları
    BeadMat("Akik", Color(0xFFB5401F), Color(0xFFE0703F), Color(0xFF6E1A0A), Color(0xFFF09A6A), Color(0xFF7A2210), 0.9f, true, BeadPattern.BANDS),
    // Kaplan gözü: ışıkla kayan ipeksi altın bant
    BeadMat("Kaplan Gözü", Color(0xFF8A5A1C), Color(0xFFD49A3E), Color(0xFF3E2408), Color(0xFFF0C060), Color(0xFF4A2C0C), 0.9f, true, BeadPattern.CHATOYANT),
    // Lapis lazuli: lacivert, altın pirit zerreleri
    BeadMat("Lapis", Color(0xFF1F3A93), Color(0xFF3C62C8), Color(0xFF0A1745), Color(0xFF5577D8), Color(0xFF0F1F5C), 0.8f, false, BeadPattern.FLECKS),
    // Turkuaz: mat, kahverengi damar ağı
    BeadMat("Turkuaz", Color(0xFF3FB3B0), Color(0xFF7FD8D2), Color(0xFF1E6F70), Color(0xFFA5E8E0), Color(0xFF2A8A88), 0.5f, true, BeadPattern.VEINS),
    // Ametist: yarı saydam mor
    BeadMat("Ametist", Color(0xE07B3FA8), Color(0xF2C08BE8), Color(0xFF3A1260), Color(0xFFD9B2F5), Color(0xFF4E1F7A), 1f, true, BeadPattern.NONE),
    // Pembe kuvars: süt pembesi
    BeadMat("Pembe Kuvars", Color(0xFFE8A9B8), Color(0xFFF8D3DC), Color(0xFFB06A80), Color(0xFFFFE6EC), Color(0xFFC88596), 0.7f, false, BeadPattern.NONE),
    // Yeşim: yağlı parlaklıkta yumuşak yeşil
    BeadMat("Yeşim", Color(0xFF3E8E5E), Color(0xFF74C08E), Color(0xFF17462B), Color(0xFF9AD8AE), Color(0xFF245F3C), 0.7f, false, BeadPattern.NONE),
    // Malakit: iç içe koyu-açık yeşil halkalar
    BeadMat("Malakit", Color(0xFF1E7A4E), Color(0xFF3FAF77), Color(0xFF08331F), Color(0xFF5CCB92), Color(0xFF0B4A2C), 0.8f, false, BeadPattern.BANDS),
    // Hematit: metalik gri
    BeadMat("Hematit", Color(0xFF4A4D52), Color(0xFF9DA2AA), Color(0xFF16171A), Color(0xFFC0C5CC), Color(0xFF222428), 1f, true, BeadPattern.NONE),
    // Obsidyen: camsı siyah
    BeadMat("Obsidyen", Color(0xFF121318), Color(0xFF3A3D48), Color(0xFF000000), Color(0xFF5A6070), Color(0xFF050506), 1f, true, BeadPattern.NONE),
    // Oniks: siyah, ince beyaz katmanlar
    BeadMat("Oniks", Color(0xFF2A2522), Color(0xFF6A625C), Color(0xFF0A0807), Color(0xFFD8D0C4), Color(0xFF0A0807), 0.9f, true, BeadPattern.BANDS),
    // Ay taşı: sütlü, mavi ışıltı
    BeadMat("Ay Taşı", Color(0xEBDDE3EE), Color(0xFFFFFFFF), Color(0xFF8E9BB5), Color(0xFFB9D2FF), Color(0xFFC8CFDC), 0.9f, false, BeadPattern.SHEEN),
    // Mercan: canlı kırmızı-turuncu
    BeadMat("Mercan", Color(0xFFD9483B), Color(0xFFF27C66), Color(0xFF8A1E16), Color(0xFFFF9E88), Color(0xFFA0281E), 0.8f, false, BeadPattern.NONE),
    // Sitrin: saydam bal sarısı
    BeadMat("Sitrin", Color(0xE0E8B020), Color(0xFFFFE27A), Color(0xFF9A6406), Color(0xFFFFF0A8), Color(0xFFB07A10), 1f, false, BeadPattern.NONE),
    // Yakut: derin kan kırmızısı
    BeadMat("Yakut", Color(0xEB9E1030), Color(0xFFE0405E), Color(0xFF4A0414), Color(0xFFFF7A90), Color(0xFF5E0A1C), 1f, true, BeadPattern.NONE),
)

/** Kubbe tuvalinin yükseklik/genişlik oranı (alttaki imame ve püskül için). */
const val DOME_H = 1.16f

/** Kubbe kehribarının üç tonu. */
class AmberTone(val light: Color, val base: Color, val edge: Color)

private val AMBER_CLOUDS = listOf(
    Quad(-0.32f, 0.10f, 0.42f, true), Quad(0.28f, -0.30f, 0.36f, false),
    Quad(0.05f, 0.42f, 0.38f, false), Quad(0.38f, 0.22f, 0.30f, true),
    Quad(-0.20f, -0.45f, 0.30f, true),
)

data class Quad(val a: Float, val b: Float, val c: Float, val d: Boolean)

/** Sayı arttıkça (0 → 1) açık bal kehribarından derin konyak kehribarına. */
fun amberAt(t: Float): AmberTone {
    val x = t.coerceIn(0f, 1f)
    return AmberTone(
        // Kullanıcının seçtiği "altın sarısı" kehribar: açık altından koyu altın-bal tonuna
        // Kullanıcının tespihine göre: hardal-okra kehribar, sayı arttıkça koyulaşır
        light = lerp(Color(0xFFEDB451), Color(0xFFD3933A), x),
        base = lerp(Color(0xFFDDA03A), Color(0xFFB9781E), x),
        edge = lerp(Color(0xFFA2681A), Color(0xFF74420C), x),
    )
}

/** Kubbe üzerindeki yazı koyu kehribarda açık, açık kehribarda koyu olur. */
fun amberIsDark(@Suppress("UNUSED_PARAMETER") t: Float): Boolean = false  // altın tonda koyu yazı her durumda okunur (≥ 5,8:1)

private fun Color.darker(f: Float) = Color(red * (1 - f), green * (1 - f), blue * (1 - f), alpha)

/** Kubbe tonu: kehribarda okra → bal; diğer malzemelerde kendi renginden biraz koyusuna. */
fun domeTone(m: BeadMat, t: Float): AmberTone {
    if (m.name == "Kehribar") return amberAt(t)
    val x = t.coerceIn(0f, 1f)
    return AmberTone(
        light = lerp(m.light, m.base, 0.45f * x),
        base = lerp(m.base, m.base.darker(0.30f), x),
        edge = lerp(m.edge, m.edge.darker(0.30f), x),
    )
}

/** Oltu (siyah) ve kristal (koyu zeminde saydam) kubbede rakam açık renk olur. */
fun domeNeedsLightText(@Suppress("UNUSED_PARAMETER") m: BeadMat): Boolean = true   // berrak cam kubbe, koyu zemin

/** Çektikçe değişen renk: gece mavisi → menekşe → gül → kehribar → altın. */
private fun phase(p: Pal, t: Float): Color {
    val stops = if (p.dark) listOf(Color(0xFF7FA7F0), Color(0xFFA68CF0), Color(0xFFEA96BE), Color(0xFFF2AE62), Color(0xFFF6DDA0))
    else listOf(Color(0xFF4E74C4), Color(0xFF7A5CC6), Color(0xFFC2648E), Color(0xFFC9822F), Color(0xFFB8913F))
    val x = t.coerceIn(0f, 1f) * (stops.size - 1)
    val i = x.toInt().coerceAtMost(stops.size - 2)
    return lerp(stops[i], stops[i + 1], x - i)
}

private fun metalBrush(silver: Boolean, a: Offset, b: Offset): Brush = Brush.linearGradient(
    if (silver) listOf(Color(0xFFF5F5F7), Color(0xFF9C9CA4), Color(0xFFE4E4E8), Color(0xFF5E5E66))
    else listOf(Color(0xFFF7E4AA), Color(0xFFB88A3A), Color(0xFFEDCD82), Color(0xFF6E5018)),
    start = a, end = b,
)

/** Tane görüntüleri önbelleği: malzeme ya da boyut değişince bir kez üretilir. */
class BeadSprites {
    class Sprites(val size: Int, val beads: List<ImageBitmap>, val shadow: ImageBitmap)
    private var key: Pair<BeadMat, Int>? = null
    private var cached: Sprites? = null

    fun get(m: BeadMat, r: Float, density: Density): Sprites {
        val size = ceil(r * 2f + 4f).toInt().coerceAtLeast(4)
        val k = m to size
        cached?.let { if (key == k) return it }
        val c = Offset(size / 2f, size / 2f)
        val beads = (0 until 6).map { v -> render(size, density) { drawBead(m, c, r, v) } }
        val shadowSize = ceil(r * 2.6f + 4f).toInt()
        val sc = Offset(shadowSize / 2f, shadowSize / 2f)
        val sh = render(shadowSize, density) {
            drawCircle(Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.38f), Color.Transparent), center = sc, radius = r * 1.3f), r * 1.3f, sc)
        }
        // Gölge, tane ile aynı merkeze hizalansın diye aynı boyutta tutulur
        val shadow = if (shadowSize == size) sh else render(size, density) { drawImage(sh, Offset((size - shadowSize) / 2f, (size - shadowSize) / 2f)) }
        return Sprites(size, beads, shadow).also { cached = it; key = k }
    }

    private fun render(size: Int, density: Density, block: DrawScope.() -> Unit): ImageBitmap {
        val bmp = ImageBitmap(size, size)
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(bmp), Size(size.toFloat(), size.toFloat()), block)
        return bmp
    }
}

/**
 * Berrak kehribar "damlası": opak sıkma kehribarın içindeki ışık geçiren bölge.
 * Birbirine kaynamış üç yumuşak elips (berrak bal tonu) + ışığın geçtiği kenarda hafif sıcaklık.
 */
private fun DrawScope.drawAmberDrop(x: Float, y: Float, rr: Float, angle: Float) {
    val core = Brush.radialGradient(0f to DROP_CORE.copy(alpha = 0.32f), 0.6f to DROP_EDGE.copy(alpha = 0.20f), 1f to Color.Transparent, center = Offset.Zero, radius = 1f)
    for ((dx, dy, sx, sy) in DROP_PARTS) {
        val px = x + dx * rr
        val py = y + dy * rr
        withTransform({ translate(px, py); rotate(angle, Offset.Zero); scale(rr * sx, rr * sy, Offset.Zero) }) {
            drawCircle(core, 1f, Offset.Zero)
        }
    }
    val gx = x + rr * 0.25f
    val gy = y + rr * 0.32f
    val glow = Brush.radialGradient(listOf(DROP_GLOW.copy(alpha = 0.28f), Color.Transparent), center = Offset.Zero, radius = 1f)
    withTransform({ translate(gx, gy); rotate(angle, Offset.Zero); scale(rr * 0.7f, rr * 0.35f, Offset.Zero) }) {
        drawCircle(glow, 1f, Offset.Zero)
    }
}

private val DROP_CORE = Color(0xFFB06A0C)
private val DROP_EDGE = Color(0xFFB8760F)
private val DROP_GLOW = Color(0xFFF5BE52)
private val DROP_PARTS = listOf(Quad4(0f, 0f, 1f, 0.62f), Quad4(0.45f, 0.18f, 0.7f, 0.5f), Quad4(-0.35f, 0.22f, 0.6f, 0.42f))
private data class Quad4(val a: Float, val b: Float, val c: Float, val d: Float)

/** Tane çeşitlerine göre damla yerleşimi: (x, y, boyut) tane yarıçapı cinsinden. */
private val BEAD_DROPS = listOf(
    listOf(Triple(0.25f, 0.30f, 0.55f)), emptyList(), listOf(Triple(-0.30f, 0.28f, 0.50f)),
    listOf(Triple(0.10f, 0.45f, 0.55f)), emptyList(), listOf(Triple(0.28f, -0.05f, 0.45f)),
)

/** Doğal taş desenleri; tane dairesine kırpılmış olarak çizilir. Varyanta göre her tane biraz farklıdır. */
private fun DrawScope.drawStonePattern(m: BeadMat, q: Offset, r: Float, variant: Int) {
    val rnd = java.util.Random(variant * 7919L + m.name.hashCode())
    when (m.pattern) {
        BeadPattern.DROPS -> BEAD_DROPS[variant % BEAD_DROPS.size].forEachIndexed { k, (dx, dy, dr) ->
            drawAmberDrop(q.x + dx * r, q.y + dy * r, dr * r, 20f + k * 55f + variant * 30f)
        }
        BeadPattern.FLECKS -> {
            // pirit zerreleri ve birkaç soluk kalsit lekesi
            repeat(2) {
                val p = Offset(q.x + (rnd.nextFloat() - 0.5f) * r * 1.4f, q.y + (rnd.nextFloat() - 0.5f) * r * 1.4f)
                drawCircle(Brush.radialGradient(listOf(Color(0xFFDCE4F5).copy(alpha = 0.16f), Color.Transparent), center = p, radius = r * 0.35f), r * 0.35f, p)
            }
            repeat(9) {
                val p = Offset(q.x + (rnd.nextFloat() - 0.5f) * r * 1.7f, q.y + (rnd.nextFloat() - 0.5f) * r * 1.7f)
                val s = r * (0.03f + rnd.nextFloat() * 0.05f)
                drawCircle(Color(0xFFE9C766).copy(alpha = 0.55f + rnd.nextFloat() * 0.4f), s, p)
            }
        }
        BeadPattern.CHATOYANT -> {
            // ipeksi lifler ve ışıkla kayan parlak bant
            val ang = -35f + variant * 17f
            withTransform({ rotate(ang, q) }) {
                for (i in -7..7) {
                    val y = q.y + i * r * 0.13f
                    drawLine(m.mottleDark.copy(alpha = 0.18f), Offset(q.x - r, y), Offset(q.x + r, y), r * 0.05f)
                }
                val by = q.y + (rnd.nextFloat() - 0.5f) * r * 0.5f
                drawRect(
                    Brush.verticalGradient(0f to Color.Transparent, 0.5f to m.mottleLight.copy(alpha = 0.55f), 1f to Color.Transparent, startY = by - r * 0.32f, endY = by + r * 0.32f),
                    topLeft = Offset(q.x - r, by - r * 0.32f), size = Size(r * 2f, r * 0.64f),
                )
            }
        }
        BeadPattern.BANDS -> {
            // iç içe katman halkaları (akik, malakit, oniks)
            val cc = Offset(q.x + (rnd.nextFloat() - 0.5f) * r * 1.6f, q.y + (rnd.nextFloat() - 0.5f) * r * 1.6f)
            val onyx = m.name == "Oniks"
            var rad = r * (0.15f + rnd.nextFloat() * 0.1f)
            var i = 0
            while (rad < r * 2.4f) {
                val col = if (i % 2 == 0) m.mottleLight else m.mottleDark
                val a = if (onyx) (if (i % 2 == 0) 0.55f else 0f) else 0.30f
                if (a > 0f) drawCircle(col.copy(alpha = a), rad, cc, style = Stroke(r * (if (onyx) 0.035f else 0.07f + rnd.nextFloat() * 0.05f)))
                rad += r * (0.12f + rnd.nextFloat() * 0.12f)
                i++
            }
        }
        BeadPattern.VEINS -> {
            // turkuazın kahverengi damar ağı
            repeat(3) {
                val path = Path()
                var x = q.x + (rnd.nextFloat() - 0.5f) * r * 2f
                var y = q.y - r
                path.moveTo(x, y)
                while (y < q.y + r) {
                    x += (rnd.nextFloat() - 0.5f) * r * 0.5f
                    y += r * (0.15f + rnd.nextFloat() * 0.2f)
                    path.lineTo(x, y)
                }
                drawPath(path, Color(0xFF5A4630).copy(alpha = 0.45f), style = Stroke(r * (0.03f + rnd.nextFloat() * 0.03f), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        BeadPattern.SHEEN -> {
            // ay taşının mavi ışıltısı (adularesans)
            val p = Offset(q.x + r * 0.15f, q.y - r * 0.05f)
            drawOval(
                Brush.radialGradient(listOf(Color(0xFF8FB8FF).copy(alpha = 0.55f), Color.Transparent), center = p, radius = r * 0.7f),
                topLeft = Offset(p.x - r * 0.7f, p.y - r * 0.4f), size = Size(r * 1.4f, r * 0.8f),
            )
        }
        BeadPattern.NONE -> Unit
    }
}

/** Tek bir tane: gövde, doğal bulutlanma, kenar koyulaşması, yansıyan ışık, ipeksi parlaklık ve keskin ışık noktası. */
internal fun DrawScope.drawBead(m: BeadMat, q: Offset, r: Float, variant: Int) {
    drawCircle(
        Brush.radialGradient(0f to m.light, 0.55f to m.base, 1f to m.edge, center = Offset(q.x - r * 0.30f, q.y - r * 0.36f), radius = r * 1.45f),
        r, q,
    )
    val h1 = ((variant * 73 + 17) % 97) / 97f
    val h2 = ((variant * 41 + 59) % 89) / 89f
    val m1 = Offset(q.x + (h1 - 0.5f) * r * 0.8f, q.y + (h2 - 0.5f) * r * 0.7f)
    val m2 = Offset(q.x + (h2 - 0.5f) * r * 0.7f, q.y + (0.5f - h1) * r * 0.8f)
    val r1 = r * (0.35f + 0.2f * h2)
    val r2 = r * (0.30f + 0.2f * h1)
    drawCircle(Brush.radialGradient(listOf(m.mottleLight.copy(alpha = m.mottleLight.alpha * 0.55f), Color.Transparent), center = m1, radius = r1), r1, m1)
    drawCircle(Brush.radialGradient(listOf(m.mottleDark.copy(alpha = m.mottleDark.alpha * 0.28f), Color.Transparent), center = m2, radius = r2), r2, m2)
    if (m.pattern != BeadPattern.NONE) {
        clipPath(Path().apply { addOval(Rect(q, r)) }) { drawStonePattern(m, q, r, variant) }
    }
    drawCircle(Brush.radialGradient(0.70f to Color.Transparent, 1f to m.edge.copy(alpha = 0.55f), center = q, radius = r), r, q)
    // Gerçek kehribardaki gibi: geniş beyaz parlama yok; küçük, sıcak tonlu tek bir ışık noktası
    val bo = Offset(q.x + r * 0.12f, q.y + r * 0.52f)
    drawCircle(Brush.radialGradient(listOf(m.light.copy(alpha = 0.10f), Color.Transparent), center = bo, radius = r * 0.45f), r * 0.45f, bo)
    val so = Offset(q.x - r * 0.28f, q.y - r * 0.34f)
    drawCircle(Brush.radialGradient(listOf(WARM_SHEEN.copy(alpha = 0.10f), Color.Transparent), center = so, radius = r * 0.55f), r * 0.55f, so)
    val spt = Offset(q.x - r * 0.18f, q.y - r * 0.22f)
    drawCircle(Brush.radialGradient(listOf(WARM_HALO.copy(alpha = 0.30f * m.spec), Color.Transparent), center = spt, radius = r * 0.12f), r * 0.12f, spt)
    drawCircle(WARM_SPEC.copy(alpha = 0.85f * m.spec), r * 0.065f, spt)
}

private fun DrawScope.drawDome(p: Pal, m: BeadMat, sprites: BeadSprites, prog: Float, pos: Float, pulse: Float, burst: Float, press: Float, reduce: Boolean, back: Boolean = false) {
    val k = size.width / 380.dp.toPx()
    val gapDeg = 18f                                         // imamenin durduğu boşluk (altta)
    val seg = (360f - gapDeg) / (33f + PULL_GAP)               // 33 tane + ipteki boş aralık
    val f = PI.toFloat() * seg / 360f                        // tane yarıçapı / ip yarıçapı (birbirine değen taneler)
    // Taneler büyük: komşu taneler gerçek tespihteki gibi hafifçe üst üste biner, kubbe küçülür
    val rB = (size.width / 2f - 3.dp.toPx() * k) / (1f + f * BEAD_SCALE)
    val r = rB * f * BEAD_SCALE
    val c = Offset(size.width / 2f, size.width / 2f)         // üstte kare; alttaki fazlalık imame ve püskül için
    val rDisc = rB - r - 16.dp.toPx() * k
    val rArc = rDisc - 13.dp.toPx() * k
    val stroke = 3.4.dp.toPx() * k
    val ph = phase(p, prog)

    fun polar(rad: Float, deg: Float): Offset {
        val a = Math.toRadians(deg.toDouble())
        return Offset(c.x + rad * cos(a).toFloat(), c.y + rad * sin(a).toFloat())
    }

    // ── Hâle: rengi ilerlemeyle değişir ──
    if (back) drawCircle(
        Brush.radialGradient(listOf(ph.copy(alpha = if (p.dark) 0.30f else 0.22f), Color.Transparent), center = c, radius = rB * 1.22f),
        radius = rB * 1.22f, center = c,
    )

    // ── Kubbe: berrak cam (arka katman: gölge + ton; ön katman: kenar, yansıma, çerçeve) ──
    if (back) {
        val sh = Offset(c.x, c.y + rDisc * 0.10f)
        drawCircle(
            Brush.radialGradient(0.80f to Color.Transparent, 0.92f to Color.Black.copy(alpha = if (p.dark) 0.35f else 0.15f), 1f to Color.Transparent, center = sh, radius = rDisc * 1.14f),
            radius = rDisc * 1.14f, center = sh,
        )
        // Berrak cam: arkadaki gece mavisi ve hâle görünür; yalnızca çok hafif buz mavisi bir ton ve kalın cam kenarı
        drawCircle(
            Brush.radialGradient(
                0f to GLASS_TINT.copy(alpha = 0.10f), 0.80f to GLASS_TINT.copy(alpha = 0.06f), 1f to GLASS_EDGE.copy(alpha = 0.24f),
                center = Offset(c.x - rDisc * 0.20f, c.y - rDisc * 0.30f), radius = rDisc * 1.4f,
            ),
            radius = rDisc, center = c,
        )
        // Alt kenarda ışığın toplandığı yumuşak parlaklık (kostik)
        val caus = Offset(c.x + rDisc * 0.04f, c.y + rDisc * 0.72f)
        drawOval(
            Brush.radialGradient(listOf(GLASS_EDGE.copy(alpha = 0.40f), Color.Transparent), center = caus, radius = rDisc * 0.58f),
            topLeft = Offset(caus.x - rDisc * 0.58f, caus.y - rDisc * 0.20f), size = Size(rDisc * 1.16f, rDisc * 0.40f),
        )
        return
    }
    // Kristal: kenarda ışık kırılması (fresnel) — iç kenar boyunca ince, aydınlık bir halka
    val glass = Color(0xFFDCE8FF)
    drawCircle(
        Brush.radialGradient(0.84f to Color.Transparent, 0.965f to glass.copy(alpha = 0.34f), 1f to Color.Transparent, center = c, radius = rDisc),
        rDisc, c,
    )
    // Sol üstte ince hilal yansıma (cam yüzeyi); büyük beyaz parlama yok
    run {
        val rr = rDisc * 0.90f
        drawArc(
            Brush.sweepGradient(
                0f to Color.Transparent, 0.52f to Color.Transparent, 0.60f to glass.copy(alpha = 0.10f),
                0.67f to glass.copy(alpha = 0.36f), 0.74f to glass.copy(alpha = 0.10f), 0.82f to Color.Transparent, 1f to Color.Transparent,
                center = c,
            ),
            startAngle = 190f, sweepAngle = 95f, useCenter = false,
            topLeft = Offset(c.x - rr, c.y - rr), size = Size(rr * 2, rr * 2),
            style = Stroke(rDisc * 0.035f, cap = StrokeCap.Round),
        )
    }
    // ince altın çerçeve
    drawCircle(
        Brush.sweepGradient(listOf(p.goldLight, Color.White, p.gold, p.goldDeep, Color.White.copy(alpha = 0.85f), p.goldLight), center = c),
        radius = rDisc, center = c, style = Stroke(1.6.dp.toPx() * k), alpha = 0.85f,
    )
    if (press > 0f) drawCircle(Color.Black.copy(alpha = 0.12f * press), rDisc, c)
    if (pulse < 1f) {
        val a = 1f - pulse
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(alpha = a * 0.20f), Color.Transparent), center = c, radius = rDisc),
            radius = rDisc, center = c,
        )
    }

    // ── İlerleme yayı (kehribar üzerinde krem rengi) ──
    drawCircle(Color.White.copy(alpha = 0.07f), rArc, c, style = Stroke(stroke))
    val sweep = 360f * prog
    if (sweep > 0.4f) {
        val tl = Offset(c.x - rArc, c.y - rArc)
        val sz = Size(rArc * 2, rArc * 2)
        rotate(-90f, c) {
            drawArc(Color(0xFFFFF4D6).copy(alpha = 0.18f), 0f, sweep, false, tl, sz, style = Stroke(stroke * 2.4f, cap = StrokeCap.Round))
            drawArc(Color(0xFFFFF1CC).copy(alpha = 0.92f), 0f, sweep, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        val head = polar(rArc, -90f + sweep)
        drawCircle(Color.White.copy(alpha = 0.35f), stroke * 2.4f, head)
        drawCircle(Color.White, stroke * 0.85f, head)
    }

    // ── Tespih ipi ──
    // Gerçek tespih ipi gibi: iki kat bükülmüş ipek; koyu gövde üstünde çapraz dönen iplik parlamaları
    run {
        val cw = 2.6.dp.toPx() * k
        drawCircle(Color.Black.copy(alpha = 0.35f), rB, Offset(c.x, c.y + cw * 0.35f), style = Stroke(cw * 1.1f))
        drawCircle(CORD_DARK, rB, c, style = Stroke(cw))
        val n = (2f * PI.toFloat() * rB / (cw * 1.05f)).toInt()
        val hl = cw * 0.52f
        for (i in 0 until n) {
            val ang = i * 360f / n
            val q = polar(rB, ang)
            val d = Math.toRadians((ang + 90f + 42f).toDouble())
            val dx = cos(d).toFloat() * hl
            val dy = sin(d).toFloat() * hl
            drawLine(CORD_LIGHT, Offset(q.x - dx, q.y - dy), Offset(q.x + dx, q.y + dy), cw * 0.40f, cap = StrokeCap.Round)
            drawLine(CORD_SHINE, Offset(q.x - dx * 0.35f, q.y - dy * 0.35f), Offset(q.x + dx * 0.15f, q.y + dy * 0.15f), cw * 0.16f, cap = StrokeCap.Round)
        }
    }

    // ── Taneler: gerçek tespih mekaniği ──
    // İp üzerinde konum (v, tane biriminde, imamenin sağından saat yönünün tersine):
    //   [0, K)        → 2: çekilip biriken taneler (imamenin sağında)
    //   [K, K+G)      → ipte boş aralık; çekilen tane buradan düşer
    //   [K+G, 33+G)   → 1'den başlayıp halkayı dolanan, sırasını bekleyen taneler
    //   [33+G, Ltot)  → imamenin arkası; en alttaki tane buradan diğer tarafa geçer
    val gapSlots = gapDeg / seg
    val ltot = 33f + PULL_GAP + gapSlots
    fun slotOf(j: Float): Float = when {
        j < CLUSTER - 1 -> j + 0.5f
        j < CLUSTER -> (CLUSTER - 0.5f) + (j - (CLUSTER - 1)) * (PULL_GAP + 1f)
        j < 32f -> j + PULL_GAP + 0.5f
        else -> 32f + PULL_GAP + 0.5f + (j - 32f) * (gapSlots + 1f)
    }
    fun angleOf(v: Float) = 90f - gapDeg / 2f - v * seg
    fun jOf(id: Int) = (((id - pos) % 33f) + 33f) % 33f

    class BeadAt(val id: Int, val j: Float, val v: Float, val q: Offset, val a: Float)
    val beads = ArrayList<BeadAt>(33)
    for (id in 0 until 33) {
        val j = jOf(id)
        val v = slotOf(j)
        val vm = ((v % ltot) + ltot) % ltot
        val a = if (vm > 33f + PULL_GAP) {
            val dist = minOf(vm - (33f + PULL_GAP), ltot - vm)
            1f - (dist / 0.8f).coerceIn(0f, 1f)          // imamenin arkasından geçerken gizlenir
        } else 1f
        if (a <= 0.01f) continue
        beads.add(BeadAt(id, j, v, polar(rB, angleOf(v)), a))
    }
    // Taneler önbellekteki görüntülerden çizilir (her karede yeniden fırça oluşturulmaz)
    val sp = sprites.get(m, r, this)
    val half = sp.size / 2f
    for (b in beads) {
        drawImage(sp.shadow, Offset(b.q.x - half, b.q.y - half + r * 0.38f), alpha = b.a * (if (p.dark) 1f else 0.45f))
    }
    val litUpTo = prog * 33f
    for (b in beads) {
        // Bu turda çekilmiş taneler (en son çekilen 2'de, öncekiler imamenin öbür yanına geçmiş) ilerleme renginde ışıldar
        val recent = ((((CLUSTER - 1) - b.j) % 33f) + 33f) % 33f
        val lit = recent + 0.5f <= litUpTo + 0.01f
        if (lit) {
            val g = phase(p, (recent + 0.5f) / 33f)
            drawCircle(Brush.radialGradient(listOf(g.copy(alpha = 0.50f * b.a), Color.Transparent), center = b.q, radius = r * 1.7f), r * 1.7f, b.q)
        }
        // Düşen tane: arkasında hızla orantılı, solan bir iz; inerken (2'ye değerken) kısa bir parıltı
        if (b.j > CLUSTER - 1 && b.j < CLUSTER && !reduce) {
            val s = CLUSTER - b.j                                   // 0: 1'den kopuş, 1: 2'ye iniş
            for (k in 1..3) {
                val tq = polar(rB, angleOf(b.v + k * 0.32f * (0.4f + s)))
                drawCircle(PULL_TRAIL.copy(alpha = (0.24f - 0.06f * k) * s), r * (1f - 0.1f * k), tq)
            }
        }
        if (b.j <= CLUSTER - 1 && b.j > CLUSTER - 1.25f && !reduce) {
            val impact = 1f - (CLUSTER - 1 - b.j) / 0.25f
            drawCircle(Brush.radialGradient(listOf(PULL_TRAIL.copy(alpha = 0.45f * impact), Color.Transparent), center = b.q, radius = r * 1.8f), r * 1.8f, b.q)
        }
    }
    // Üst üste binen taneler: aşağıdakiler öndedir (derinlik hissi)
    for (b in beads.sortedBy { it.q.y }) {
        val recent = ((((CLUSTER - 1) - b.j) % 33f) + 33f) % 33f
        val lit = recent + 0.5f <= litUpTo + 0.01f
        drawImage(sp.beads[(b.id * 5) % sp.beads.size], Offset(b.q.x - half, b.q.y - half), alpha = b.a)
        if (lit) drawCircle(phase(p, (recent + 0.5f) / 33f).copy(alpha = 0.10f * b.a), r, b.q)
    }
    // ── İmame: telkari başlık + uzun tane + telkari uç + halka + püskül ──
    val ir = minOf(r, 13.dp.toPx() * k)
    val x0 = c.x
    var y = c.y + rB - ir * 0.55f
    // üst telkari koni
    val capH = ir * 0.95f
    val capPath = Path().apply {
        moveTo(x0 - ir * 0.62f, y); lineTo(x0 + ir * 0.62f, y)
        lineTo(x0 + ir * 0.30f, y + capH); lineTo(x0 - ir * 0.30f, y + capH); close()
    }
    drawPath(capPath, metalBrush(m.silver, Offset(x0 - ir, y), Offset(x0 + ir, y + capH)))
    for (j in -2..2) drawLine(Color.Black.copy(alpha = 0.35f), Offset(x0 + j * ir * 0.22f, y + capH * 0.12f), Offset(x0 + j * ir * 0.11f, y + capH * 0.9f), 0.6.dp.toPx() * k)
    y += capH
    // uzun gövde tanesi
    val bodyH = ir * 2.3f
    val bodyW = ir * 1.35f
    drawOval(
        Brush.radialGradient(0f to m.light, 0.55f to m.base, 1f to m.edge, center = Offset(x0 - bodyW * 0.18f, y + bodyH * 0.30f), radius = bodyH * 0.75f),
        topLeft = Offset(x0 - bodyW / 2, y), size = Size(bodyW, bodyH),
    )
    drawOval(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), center = Offset(x0 - bodyW * 0.18f, y + bodyH * 0.28f), radius = bodyW * 0.5f), topLeft = Offset(x0 - bodyW / 2, y), size = Size(bodyW, bodyH))
    drawCircle(Color.White.copy(alpha = 0.9f * m.spec), ir * 0.09f, Offset(x0 - bodyW * 0.2f, y + bodyH * 0.26f))
    y += bodyH - ir * 0.15f
    // alt telkari koni
    val cap2 = Path().apply {
        moveTo(x0 - ir * 0.42f, y); lineTo(x0 + ir * 0.42f, y)
        lineTo(x0 + ir * 0.14f, y + ir * 0.8f); lineTo(x0 - ir * 0.14f, y + ir * 0.8f); close()
    }
    drawPath(cap2, metalBrush(m.silver, Offset(x0 - ir, y), Offset(x0 + ir, y + ir)))
    y += ir * 0.8f
    // halka
    drawCircle(metalBrush(m.silver, Offset(x0 - ir, y), Offset(x0 + ir, y + ir)), ir * 0.24f, Offset(x0, y + ir * 0.22f), style = Stroke(1.4.dp.toPx() * k))
    y += ir * 0.46f
    // püskül: dokununca hafifçe sallanır
    val sway = if (reduce || pulse >= 1f) 0f else sin(pulse * 2f * PI.toFloat() * 1.5f) * (1f - pulse) * 9f
    val tassel = 24.dp.toPx() * k
    val silk = if (m.silver) Color(0xFFD9A441) else p.gold
    rotate(sway, Offset(x0, y)) {
        for (j in 0 until 15) {
            val t = (j - 7) / 7f
            val endX = x0 + t * ir * 0.9f
            drawLine(
                Brush.verticalGradient(listOf(silk, silk.copy(alpha = 0.55f)), startY = y, endY = y + tassel),
                Offset(x0 + t * ir * 0.18f, y), Offset(endX, y + tassel * (0.92f + 0.08f * (1f - abs(t)))), 1.1.dp.toPx() * k, cap = StrokeCap.Round,
            )
        }
        drawCircle(metalBrush(m.silver, Offset(x0 - ir, y), Offset(x0 + ir, y + ir)), ir * 0.2f, Offset(x0, y + ir * 0.05f))
    }

    // ── Hedef kutlaması ──
    if (burst < 1f) {
        val a = 1f - burst
        drawCircle(ph.copy(alpha = a * 0.7f), rArc, c, style = Stroke(stroke * (1f + 2.5f * burst)))
        if (!reduce) {
            val e = 1f - (1f - burst) * (1f - burst)
            for (j in 0 until 28) {
                val ang = j * (360f / 28f) + (j % 3) * 4f
                val dist = rDisc * 0.85f + (rB * 1.12f - rDisc * 0.85f) * e * (0.7f + 0.3f * ((j * 7) % 5) / 4f)
                val q = polar(dist, ang)
                val rr = (1.6f + (j % 4) * 0.6f) * k * density * a
                val col = phase(p, (j % 5) / 4f)
                if (j % 3 == 0) {
                    val L = rr * 3.2f
                    drawLine(col.copy(alpha = a), Offset(q.x - L, q.y), Offset(q.x + L, q.y), rr * 0.55f, cap = StrokeCap.Round)
                    drawLine(col.copy(alpha = a), Offset(q.x, q.y - L), Offset(q.x, q.y + L), rr * 0.55f, cap = StrokeCap.Round)
                } else {
                    drawCircle(col.copy(alpha = a), rr, q)
                }
            }
        }
    }
}

// ───────────── Alt kontroller ─────────────
@Composable
private fun ControlRow(st: AppState, d: Disp, sheet: (SheetKind) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        GlyphButton(G.UNDO, L("Bir eksilt", "Undo one")) { st.undo() }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (d.vird == null) {
                GoldChip(Lf("Hedef  {0}", "Target  {0}", fmt(d.target)), false, G.EDIT) { sheet(SheetKind.Target) }
            } else {
                GoldChip(L("Sonraki adım", "Next step"), false, G.SKIP) { st.skipStep() }
            }
        }
        GlyphButton(G.RESET, L("Sıfırla", "Reset")) { st.resetCurrent() }
    }
}

@Composable
private fun StatStrip(st: AppState) {
    val p = LocalPal.current
    val goal = st.settings.dailyGoal
    val today = st.today()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Ornament(220.dp, 0.8f)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat(if (goal > 0) Lf("BUGÜN · {0}", "TODAY · {0}", fmt(goal)) else L("BUGÜN", "TODAY"), fmt(today))
            Stat(L("SERİ", "STREAK"), if (st.streak() == 1) L("1 gün", "1 day") else Lf("{0} gün", "{0} days", st.streak()))
            Stat(L("TOPLAM", "TOTAL"), fmt(st.total))
        }
        if (goal > 0) {
            Spacer(Modifier.height(8.dp))
            val f = (today / goal.toFloat()).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth(0.6f).height(3.dp).clip(RoundedCornerShape(50)).background(p.faint)) {
                Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(RoundedCornerShape(50)).background(p.goldBrush))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    val p = LocalPal.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = labelStyle(p.sub).copy(fontSize = 10.5.sp, letterSpacing = 1.4.sp, fontFeatureSettings = NUM_FEATURES))
        Spacer(Modifier.height(2.dp))
        Text(value, style = TextStyle(brush = p.numberBrush, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, fontFeatureSettings = NUM_FEATURES))
    }
}
