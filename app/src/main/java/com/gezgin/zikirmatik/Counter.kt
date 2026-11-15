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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
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
fun fmt(n: Int): String = String.format(TR, "%,d", n)

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
            GlyphButton(G.LIST, "Zikirlerim") { go(Scr.ZIKIRLER) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "ZİKİRMATİK",
                    style = TextStyle(brush = p.goldBrush, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, letterSpacing = 7.sp),
                    modifier = Modifier.padding(start = 7.dp),
                )
                Spacer(Modifier.height(4.dp))
                Ornament(88.dp)
            }
            GlyphButton(G.GRID, "Menü") { sheet(SheetKind.Menu) }
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
                    contentDescription = "${d.okunus}. ${d.shown}, hedef ${d.target}. Saymak için dokunun."
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
            Dome(d, pressed, st.pulseKey, st.celebrateKey, st.shakeKey, Modifier.fillMaxSize().padding(vertical = 8.dp))
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
                if (d.vird == null && n > 1) GlyphButton(G.LEFT, "Önceki zikir", 40.dp, bordered = false) { st.step(-1) } else Spacer(Modifier.width(12.dp))
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
                                    fontSize = when { z.arapca.length > 70 -> 20.sp; z.arapca.length > 40 -> 24.sp; else -> 31.sp },
                                    lineHeight = when { z.arapca.length > 70 -> 35.sp; z.arapca.length > 40 -> 42.sp; else -> 54.sp },
                                    color = p.text, textAlign = TextAlign.Center, textDirection = TextDirection.Rtl,
                                ),
                                maxLines = 5,
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
                if (d.vird == null && n > 1) GlyphButton(G.RIGHT, "Sonraki zikir", 40.dp, bordered = false) { st.step(1) } else Spacer(Modifier.width(12.dp))
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
        GlyphButton(G.CLOSE, "Tesbihatı bitir", 38.dp, bordered = false) { st.endSession() }
    }
}

// ───────────── Kristal kubbe ─────────────
@Composable
private fun Dome(d: Disp, pressed: Boolean, pulseKey: Int, celebrateKey: Int, shakeKey: Int, modifier: Modifier) {
    val p = LocalPal.current
    val shown = d.shown
    val reduce = rememberReducedMotion()

    val prog = remember { Animatable(0f) }
    LaunchedEffect(shown, d.target, d.key) {
        val t = d.progress
        if (t < prog.value - 0.4f) prog.snapTo(0f)
        if (reduce) prog.snapTo(t) else prog.animateTo(t, spring(stiffness = Spring.StiffnessMediumLow))
    }
    val beadStep = 360f / 33f
    val rot = remember { Animatable(0f) }
    LaunchedEffect(d.count, d.key) {
        val t = d.count * beadStep
        if (reduce || abs(t - rot.value) > 400f) rot.snapTo(t) else rot.animateTo(t, spring(dampingRatio = 0.62f, stiffness = 520f))
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
        val side = minOf(maxWidth, maxHeight, 360.dp)
        val dens = LocalDensity.current
        val numSize = with(dens) { (side * (if (shown >= 1000) 0.21f else 0.285f)).toSp() }
        val small = with(dens) { (side * 0.036f).toSp() }
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
            Canvas(Modifier.fillMaxSize()) { drawDome(p, prog.value, rot.value, pulse.value, burst.value, press, reduce) }
            // Rakam her zaman tam ortada; diğer yazılar üst üste bindirilir
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    "HEDEF  ${fmt(d.target)}",
                    style = labelStyle(p.sub).copy(fontSize = small, fontFeatureSettings = NUM_FEATURES),
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
            Text(
                "TAMAMLANDI",
                style = labelStyle(p.goldText).copy(fontSize = small, letterSpacing = 3.sp),
                modifier = Modifier.align(Alignment.Center).offset(y = -(side * 0.235f)).padding(start = 3.dp)
                    .graphicsLayer { val b = burst.value; alpha = if (b < 1f) sin(PI.toFloat() * b).coerceIn(0f, 1f) else 0f },
            )
            if (d.laps > 0) {
                Text(
                    "${d.laps}. TUR",
                    style = labelStyle(p.onGold).copy(fontWeight = FontWeight.Bold, fontFeatureSettings = NUM_FEATURES),
                    modifier = Modifier.align(Alignment.Center).offset(y = side * 0.235f)
                        .clip(RoundedCornerShape(50)).background(p.goldBrush).padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

private fun DrawScope.drawDome(p: Pal, prog: Float, rot: Float, pulse: Float, burst: Float, press: Float, reduce: Boolean) {
    val c = center
    val k = size.minDimension / 320.dp.toPx()
    val rOuter = size.minDimension / 2 - 10.dp.toPx() * k
    val beadR = 5.8.dp.toPx() * k
    val rBeads = rOuter - beadR - 2.dp.toPx() * k
    val rDisc = rBeads - beadR - 11.dp.toPx() * k
    val rArc = rDisc - 14.dp.toPx() * k
    val stroke = 3.6.dp.toPx() * k

    fun polar(r: Float, deg: Float): Offset {
        val a = Math.toRadians(deg.toDouble())
        return Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
    }

    // Arka hâle
    drawCircle(
        Brush.radialGradient(listOf(p.domeGlow.copy(alpha = if (p.dark) 0.30f else 0.22f), Color.Transparent), center = c, radius = rOuter * 1.18f),
        radius = rOuter * 1.18f, center = c,
    )
    // Zemin gölgesi (kubbenin yüzdüğü hissi)
    val sh = Offset(c.x, c.y + rDisc * 0.10f)
    drawCircle(
        Brush.radialGradient(listOf(Color.Black.copy(alpha = if (p.dark) 0.45f else 0.14f), Color.Transparent), center = sh, radius = rDisc * 1.12f),
        radius = rDisc * 1.12f, center = sh,
    )

    // Tespih ipi
    drawCircle(p.gold.copy(alpha = 0.55f), rBeads, c, style = Stroke(0.9.dp.toPx() * k))

    // 33 tespih tanesi (her dokunuşta bir tane kayar)
    val stepDeg = 360f / 33f
    // Tek fırça, her taneye kaydırılarak kullanılır (her karede 33 fırça oluşturmaz)
    val beadBrush = Brush.radialGradient(
        listOf(Color.White.copy(alpha = 0.95f), p.beadMid, p.beadEdge),
        center = Offset(-beadR * 0.35f, -beadR * 0.40f),
        radius = beadR * 1.35f,
    )
    val beadShadow = Color.Black.copy(alpha = if (p.dark) 0.35f else 0.12f)
    for (i in 0 until 33) {
        val ang = -90f + i * stepDeg + rot
        var rel = ((ang + 90f) % 360f + 360f) % 360f
        if (rel > 180f) rel -= 360f
        val near = (1f - abs(rel) / stepDeg).coerceIn(0f, 1f)
        val pos = polar(rBeads, ang)
        drawCircle(beadShadow, beadR, Offset(pos.x, pos.y + beadR * 0.45f))
        if (near > 0f) drawCircle(p.goldLight.copy(alpha = 0.45f * near), beadR * 2.3f, pos)
        translate(pos.x, pos.y) { drawCircle(beadBrush, beadR, Offset.Zero) }
        if (near > 0f) drawCircle(p.goldLight.copy(alpha = 0.55f * near), beadR, pos)
        drawCircle(Color.White.copy(alpha = 0.85f), beadR * 0.24f, Offset(pos.x - beadR * 0.35f, pos.y - beadR * 0.40f))
    }
    // İmame işareti (üstte altın baklava)
    val top = Offset(c.x, c.y - rOuter - 1.dp.toPx() * k)
    val dm = 4.5.dp.toPx() * k
    drawPath(
        Path().apply { moveTo(top.x, top.y - dm * 0.2f); lineTo(top.x + dm * 0.7f, top.y + dm * 0.6f); lineTo(top.x, top.y + dm * 1.6f); lineTo(top.x - dm * 0.7f, top.y + dm * 0.6f); close() },
        p.goldBrush,
    )

    // Kristal kubbe gövdesi
    drawCircle(
        Brush.radialGradient(
            listOf(p.dome0, p.dome1, p.dome2),
            center = Offset(c.x - rDisc * 0.30f, c.y - rDisc * 0.38f),
            radius = rDisc * 1.5f,
        ),
        radius = rDisc, center = c,
    )
    // İçten geçen ışık (sağ alt)
    drawCircle(
        Brush.radialGradient(
            listOf(p.domeGlow.copy(alpha = if (p.dark) 0.38f else 0.28f), Color.Transparent),
            center = Offset(c.x + rDisc * 0.38f, c.y + rDisc * 0.55f),
            radius = rDisc * 0.85f,
        ),
        radius = rDisc, center = c,
    )
    // Kesme yüzeyler
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
    // Prizma kenar
    drawCircle(
        Brush.sweepGradient(
            listOf(p.goldLight, Color.White, p.gold, p.goldDeep, Color.White.copy(alpha = 0.85f), p.goldLight),
            center = c,
        ),
        radius = rDisc, center = c, style = Stroke(1.7.dp.toPx() * k), alpha = if (p.dark) 0.85f else 0.9f,
    )

    // Basma: kubbe hafifçe kararır
    if (press > 0f) drawCircle(Color.Black.copy(alpha = 0.10f * press), rDisc, c)

    // Dokunuş dalgası
    if (pulse < 1f) {
        val a = 1f - pulse
        drawCircle(p.goldLight.copy(alpha = a * 0.5f), rArc + (rOuter - rArc + 16.dp.toPx() * k) * pulse, c, style = Stroke(2.dp.toPx() * k * a + 0.6f))
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(alpha = a * (if (p.dark) 0.16f else 0.30f)), Color.Transparent), center = c, radius = rDisc),
            radius = rDisc, center = c,
        )
    }

    // İlerleme yayı
    drawCircle(if (p.dark) Color.White.copy(alpha = 0.10f) else p.goldDeep.copy(alpha = 0.12f), rArc, c, style = Stroke(stroke))
    val sweep = 360f * prog
    if (sweep > 0.4f) {
        val tl = Offset(c.x - rArc, c.y - rArc)
        val sz = Size(rArc * 2, rArc * 2)
        rotate(-90f, c) {
            drawArc(p.gold.copy(alpha = 0.14f), 0f, sweep, false, tl, sz, style = Stroke(stroke * 2.2f, cap = StrokeCap.Round))
            drawArc(
                Brush.sweepGradient(0f to p.goldDeep, 0.5f to p.gold, 1f to p.goldLight, center = c),
                0f, sweep, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        val head = polar(rArc, -90f + sweep)
        drawCircle(p.goldLight.copy(alpha = 0.35f), stroke * 2.4f, head)
        drawCircle(Color.White, stroke * 0.85f, head)
    }
    drawCircle(if (p.dark) Color.White.copy(alpha = 0.06f) else p.goldDeep.copy(alpha = 0.10f), rArc - 12.dp.toPx() * k, c, style = Stroke(0.8.dp.toPx() * k))

    // Cam parıltısı
    rotate(-35f, c) {
        drawOval(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = (if (p.dark) 0.20f else 0.55f) * (1f - 0.5f * press)), Color.Transparent),
                startY = c.y - rDisc * 0.95f, endY = c.y - rDisc * 0.40f,
            ),
            topLeft = Offset(c.x - rDisc * 0.6f, c.y - rDisc * 0.95f),
            size = Size(rDisc * 1.2f, rDisc * 0.55f),
        )
    }

    // Hedef kutlaması: altın parçacıklar
    if (burst < 1f) {
        val a = 1f - burst
        drawCircle(p.goldLight.copy(alpha = a * 0.7f), rArc, c, style = Stroke(stroke * (1f + 2.5f * burst)))
        val e = 1f - (1f - burst) * (1f - burst)
        if (!reduce) for (j in 0 until 28) {
            val ang = j * (360f / 28f) + (j % 3) * 4f
            val dist = rDisc * 0.85f + (rOuter * 1.08f - rDisc * 0.85f) * e * (0.7f + 0.3f * ((j * 7) % 5) / 4f)
            val q = polar(dist, ang)
            val r = (1.6f + (j % 4) * 0.6f) * k * density * a
            if (j % 3 == 0) {
                val L = r * 3.2f
                drawLine(p.goldLight.copy(alpha = a), Offset(q.x - L, q.y), Offset(q.x + L, q.y), r * 0.55f, cap = StrokeCap.Round)
                drawLine(p.goldLight.copy(alpha = a), Offset(q.x, q.y - L), Offset(q.x, q.y + L), r * 0.55f, cap = StrokeCap.Round)
            } else {
                drawCircle(p.gold.copy(alpha = a), r, q)
            }
        }
    }
}

// ───────────── Alt kontroller ─────────────
@Composable
private fun ControlRow(st: AppState, d: Disp, sheet: (SheetKind) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        GlyphButton(G.UNDO, "Bir eksilt") { st.undo() }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (d.vird == null) {
                GoldChip("Hedef  ${fmt(d.target)}", false, G.EDIT) { sheet(SheetKind.Target) }
            } else {
                GoldChip("Sonraki adım", false, G.SKIP) { st.skipStep() }
            }
        }
        GlyphButton(G.RESET, "Sıfırla") { st.resetCurrent() }
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
            Stat(if (goal > 0) "BUGÜN · ${fmt(goal)}" else "BUGÜN", fmt(today))
            Stat("SERİ", if (st.streak() == 1) "1 gün" else "${st.streak()} gün")
            Stat("TOPLAM", fmt(st.total))
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
