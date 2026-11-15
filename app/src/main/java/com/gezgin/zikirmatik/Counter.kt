package com.gezgin.zikirmatik

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
fun fmt(n: Int): String = String.format(I18n.locale, "%,d", n)

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
                    contentDescription = L("${d.okunus}. ${d.shown}, hedef ${d.target}. Saymak için dokunun.", "${d.okunus}. ${d.shown} of ${d.target}. Tap to count.")
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
                                if (drag < -90f) st.step(1) else if (drag > 90f) st.step(-1)
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
                        if (z.anlam.isNotBlank()) {
                            Spacer(Modifier.height(5.dp))
                            Text(z.anlam, style = bodyStyle(p.sub).copy(fontSize = 13.sp, textAlign = TextAlign.Center), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        if (z.fazilet.isNotBlank()) {
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
        if (reduce || abs(t - rot.value) > 33f) rot.snapTo(t) else rot.animateTo(t, spring(dampingRatio = 0.62f, stiffness = 520f))
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

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight, 400.dp)
        val dens = LocalDensity.current
        val numSize = with(dens) { (side * (if (shown >= 1000) 0.17f else 0.235f)).toSp() }
        val lift = side * (24f / 380f)
        val small = with(dens) { (side * 0.032f).toSp() }
        val px = dens.density
        Box(
            Modifier
                .size(side)
                .graphicsLayer {
                    translationX = shake.value * px
                    val sc = 1f - 0.028f * press
                    scaleX = sc; scaleY = sc
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) { drawDome(p, BEADS[bead.coerceIn(0, BEADS.lastIndex)], sprites, prog.value, rot.value, pulse.value, burst.value, press, reduce) }
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
                            brush = p.numberBrush, fontFamily = Display, fontWeight = FontWeight.Normal,
                            fontSize = numSize, fontFeatureSettings = NUM_FEATURES,
                            shadow = if (p.dark) Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 4f), 18f) else Shadow(p.goldDeep.copy(alpha = 0.18f), Offset(0f, 2f), 8f),
                        ),
                    )
                }
                Text(
                    L("HEDEF  ${fmt(d.target)}", "TARGET  ${fmt(d.target)}"),
                    style = labelStyle(p.sub).copy(fontSize = small, fontFeatureSettings = NUM_FEATURES),
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
            Text(
                L("TAMAMLANDI", "COMPLETE"),
                style = labelStyle(p.goldText).copy(fontSize = small, letterSpacing = 3.sp),
                modifier = Modifier.align(Alignment.Center).offset(y = -(side * 0.19f) - lift).padding(start = 3.dp)
                    .graphicsLayer { val b = burst.value; alpha = if (b < 1f) sin(PI.toFloat() * b).coerceIn(0f, 1f) else 0f },
            )
            if (d.laps > 0) {
                Text(
                    L("${d.laps}. TUR", "ROUND ${d.laps}"),
                    style = labelStyle(p.onGold).copy(fontWeight = FontWeight.Bold, fontFeatureSettings = NUM_FEATURES),
                    modifier = Modifier.align(Alignment.Center).offset(y = side * 0.17f - lift)
                        .clip(RoundedCornerShape(50)).background(p.goldBrush).padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** Tespih tanesi malzemesi. */
class BeadMat(
    val name: String,
    val base: Color, val light: Color, val edge: Color,
    val mottleLight: Color, val mottleDark: Color,
    val spec: Float,
    val silver: Boolean,
)

val BEADS = listOf(
    // Sıkma kehribar: opak, tereyağı sarısı, yumuşak bulutlanma, gümüş telkari
    BeadMat("Kehribar", Color(0xFFD49A2E), Color(0xFFF3C766), Color(0xFF8A5810), Color(0xFFF6D488), Color(0xFFB4761C), 0.95f, true),
    BeadMat("Oltu", Color(0xFF1C1915), Color(0xFF524940), Color(0xFF040303), Color(0xFF332C25), Color(0xFF0A0908), 1f, true),
    BeadMat("Sedef", Color(0xFFE6DED3), Color(0xFFFFFFFF), Color(0xFFA0928A), Color(0xFFF7E3EE), Color(0xFFD9E5F3), 0.8f, false),
    BeadMat("Kristal", Color(0x66DCE6FF), Color(0xEEFFFFFF), Color(0xCC5E7AB4), Color(0x88FFFFFF), Color(0x55A9BDF0), 1f, false),
)

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

/** Tek bir tane: gövde, doğal bulutlanma, kenar koyulaşması, yansıyan ışık, ipeksi parlaklık ve keskin ışık noktası. */
private fun DrawScope.drawBead(m: BeadMat, q: Offset, r: Float, variant: Int) {
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
    drawCircle(Brush.radialGradient(listOf(m.mottleDark.copy(alpha = m.mottleDark.alpha * 0.45f), Color.Transparent), center = m2, radius = r2), r2, m2)
    drawCircle(Brush.radialGradient(0.70f to Color.Transparent, 1f to m.edge.copy(alpha = 0.55f), center = q, radius = r), r, q)
    val bo = Offset(q.x + r * 0.12f, q.y + r * 0.52f)
    drawCircle(Brush.radialGradient(listOf(m.light.copy(alpha = 0.28f), Color.Transparent), center = bo, radius = r * 0.45f), r * 0.45f, bo)
    val so = Offset(q.x - r * 0.28f, q.y - r * 0.34f)
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.24f), Color.Transparent), center = so, radius = r * 0.55f), r * 0.55f, so)
    val spt = Offset(q.x - r * 0.30f, q.y - r * 0.38f)
    drawCircle(Color.White.copy(alpha = 0.35f * m.spec), r * 0.17f, spt)
    drawCircle(Color.White.copy(alpha = 0.95f * m.spec), r * 0.085f, spt)
}

private fun DrawScope.drawDome(p: Pal, m: BeadMat, sprites: BeadSprites, prog: Float, pos: Float, pulse: Float, burst: Float, press: Float, reduce: Boolean) {
    val k = size.minDimension / 380.dp.toPx()
    val c = Offset(center.x, center.y - 24.dp.toPx() * k)   // imame ve püskül için aşağıda yer aç
    val rB = size.minDimension / 2 - 42.dp.toPx() * k        // tespih ipinin yarıçapı
    val gapDeg = 30f                                         // imamenin durduğu boşluk (altta)
    val seg = (360f - gapDeg) / 33f
    val slot = (2f * PI.toFloat() * rB * seg / 360f)
    val r = slot * 0.48f                                     // birbirine değen yuvarlak taneler
    val rDisc = rB - r - 9.dp.toPx() * k
    val rArc = rDisc - 13.dp.toPx() * k
    val stroke = 3.4.dp.toPx() * k
    val ph = phase(p, prog)

    fun polar(rad: Float, deg: Float): Offset {
        val a = Math.toRadians(deg.toDouble())
        return Offset(c.x + rad * cos(a).toFloat(), c.y + rad * sin(a).toFloat())
    }

    // ── Hâle: rengi ilerlemeyle değişir ──
    drawCircle(
        Brush.radialGradient(listOf(ph.copy(alpha = if (p.dark) 0.30f else 0.22f), Color.Transparent), center = c, radius = rB * 1.22f),
        radius = rB * 1.22f, center = c,
    )

    // ── Kristal kubbe ──
    val sh = Offset(c.x, c.y + rDisc * 0.10f)
    drawCircle(
        Brush.radialGradient(listOf(Color.Black.copy(alpha = if (p.dark) 0.45f else 0.14f), Color.Transparent), center = sh, radius = rDisc * 1.12f),
        radius = rDisc * 1.12f, center = sh,
    )
    drawCircle(
        Brush.radialGradient(listOf(p.dome0, p.dome1, p.dome2), center = Offset(c.x - rDisc * 0.30f, c.y - rDisc * 0.38f), radius = rDisc * 1.5f),
        radius = rDisc, center = c,
    )
    drawCircle(
        Brush.radialGradient(listOf(ph.copy(alpha = if (p.dark) 0.40f else 0.28f), Color.Transparent), center = Offset(c.x + rDisc * 0.38f, c.y + rDisc * 0.55f), radius = rDisc * 0.9f),
        radius = rDisc, center = c,
    )
    val facet = if (p.dark) Color.White.copy(alpha = 0.04f) else p.goldDeep.copy(alpha = 0.06f)
    val rf = rDisc * 0.64f
    val oct = Path()
    for (j in 0..7) {
        val q = polar(rf, -90f + 22.5f + 45f * j)
        if (j == 0) oct.moveTo(q.x, q.y) else oct.lineTo(q.x, q.y)
    }
    oct.close()
    drawPath(oct, facet, style = Stroke(1.dp.toPx() * k))
    for (j in 0..7) {
        val a = -90f + 22.5f + 45f * j
        drawLine(facet, polar(rf, a), polar(rDisc, a), 1.dp.toPx() * k)
    }
    drawCircle(
        Brush.sweepGradient(listOf(p.goldLight, Color.White, p.gold, p.goldDeep, Color.White.copy(alpha = 0.85f), p.goldLight), center = c),
        radius = rDisc, center = c, style = Stroke(1.6.dp.toPx() * k), alpha = if (p.dark) 0.85f else 0.9f,
    )
    if (press > 0f) drawCircle(Color.Black.copy(alpha = 0.10f * press), rDisc, c)
    if (pulse < 1f) {
        val a = 1f - pulse
        drawCircle(
            Brush.radialGradient(listOf(ph.copy(alpha = a * 0.22f), Color.Transparent), center = c, radius = rDisc),
            radius = rDisc, center = c,
        )
    }

    // ── İlerleme yayı: başlangıç renginden şimdiki renge ──
    drawCircle(if (p.dark) Color.White.copy(alpha = 0.10f) else p.goldDeep.copy(alpha = 0.12f), rArc, c, style = Stroke(stroke))
    val sweep = 360f * prog
    if (sweep > 0.4f) {
        val tl = Offset(c.x - rArc, c.y - rArc)
        val sz = Size(rArc * 2, rArc * 2)
        val f = prog.coerceIn(0.002f, 1f)
        rotate(-90f, c) {
            drawArc(ph.copy(alpha = 0.16f), 0f, sweep, false, tl, sz, style = Stroke(stroke * 2.4f, cap = StrokeCap.Round))
            drawArc(
                Brush.sweepGradient(0f to phase(p, 0f), f * 0.5f to phase(p, f * 0.5f), f to ph, center = c),
                0f, sweep, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        val head = polar(rArc, -90f + sweep)
        drawCircle(ph.copy(alpha = 0.40f), stroke * 2.4f, head)
        drawCircle(Color.White, stroke * 0.85f, head)
    }
    // Cam parıltısı
    rotate(-35f, c) {
        drawOval(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = (if (p.dark) 0.18f else 0.50f) * (1f - 0.5f * press)), Color.Transparent),
                startY = c.y - rDisc * 0.95f, endY = c.y - rDisc * 0.40f,
            ),
            topLeft = Offset(c.x - rDisc * 0.6f, c.y - rDisc * 0.95f),
            size = Size(rDisc * 1.2f, rDisc * 0.55f),
        )
    }

    // ── Tespih ipi ──
    drawCircle(Color(0xFFB08A50).copy(alpha = 0.55f), rB, c, style = Stroke(1.1.dp.toPx() * k))

    // ── Taneler ──
    val beadsAt = ArrayList<Pair<Int, Offset>>(33)
    val alphas = FloatArray(33)
    for (i in 0 until 33) {
        val u = (((i + pos) % 33f) + 33f) % 33f
        val v = u + 0.5f
        val edgeDist = minOf(v, 33f - v)
        val a = (edgeDist / 0.5f).coerceIn(0f, 1f)
        alphas[i] = a
        if (a <= 0.01f) continue
        val ang = 90f + gapDeg / 2f + v * seg
        beadsAt.add(i to polar(rB, ang))
    }
    // Taneler önbellekteki görüntülerden çizilir (her karede yeniden fırça oluşturulmaz)
    val sp = sprites.get(m, r, this)
    val half = sp.size / 2f
    for ((i, q) in beadsAt) {
        drawImage(sp.shadow, Offset(q.x - half, q.y - half + r * 0.38f), alpha = alphas[i] * (if (p.dark) 1f else 0.45f))
    }
    val litUpTo = prog * 33f
    for ((i, q) in beadsAt) {
        val a = alphas[i]
        val u = (((i + pos) % 33f) + 33f) % 33f
        val lit = u + 0.5f <= litUpTo + 0.01f
        if (lit) {
            val g = phase(p, (u + 0.5f) / 33f)
            drawCircle(Brush.radialGradient(listOf(g.copy(alpha = 0.55f * a), Color.Transparent), center = q, radius = r * 1.75f), r * 1.75f, q)
        }
        drawImage(sp.beads[(i * 5) % sp.beads.size], Offset(q.x - half, q.y - half), alpha = a)
        if (lit) drawCircle(phase(p, (u + 0.5f) / 33f).copy(alpha = 0.10f * a), r, q)
    }
    // ── Nişaneler: 11. ve 22. tanelerden sonra telkari ara parçalar ──
    for (b in listOf(11, 22)) {
        val u = (((b + pos) % 33f) + 33f) % 33f
        val edgeDist = minOf(u, 33f - u)
        if (edgeDist < 0.3f) continue
        val ang = 90f + gapDeg / 2f + u * seg
        val q = polar(rB, ang)
        rotate(ang, q) {
            val w = r * 0.42f
            val h = r * 0.95f
            drawRoundRect(
                metalBrush(m.silver, Offset(q.x - w, q.y - h), Offset(q.x + w, q.y + h)),
                topLeft = Offset(q.x - w / 2, q.y - h / 2), size = Size(w, h), cornerRadius = CornerRadius(w * 0.3f),
            )
            drawLine(Color.Black.copy(alpha = 0.35f), Offset(q.x - w * 0.15f, q.y - h * 0.35f), Offset(q.x - w * 0.15f, q.y + h * 0.35f), 0.7.dp.toPx() * k)
            drawLine(Color.Black.copy(alpha = 0.35f), Offset(q.x + w * 0.15f, q.y - h * 0.35f), Offset(q.x + w * 0.15f, q.y + h * 0.35f), 0.7.dp.toPx() * k)
        }
    }

    // ── İmame: telkari başlık + uzun tane + telkari uç + halka + püskül ──
    val ir = minOf(r, 12.dp.toPx() * k)
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
                GoldChip(L("Hedef  ${fmt(d.target)}", "Target  ${fmt(d.target)}"), false, G.EDIT) { sheet(SheetKind.Target) }
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
            Stat(if (goal > 0) L("BUGÜN · ${fmt(goal)}", "TODAY · ${fmt(goal)}") else L("BUGÜN", "TODAY"), fmt(today))
            Stat(L("SERİ", "STREAK"), if (st.streak() == 1) L("1 gün", "1 day") else L("${st.streak()} gün", "${st.streak()} days"))
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
