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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
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
                )
                Spacer(Modifier.height(4.dp))
                Ornament(88.dp)
            }
            GlyphButton(G.GRID, "Menü") { sheet(SheetKind.Menu) }
        }
        Spacer(Modifier.height(16.dp))

        ZikirCard(st, d)

        // Dokunma alanı: kubbe ve çevresindeki tüm boşluk sayar
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Say. ${d.okunus}, ${d.shown} / ${d.target}"
                    role = Role.Button
                }
                .clickable(interactionSource = interaction, indication = null) { st.tap() },
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
                                    fontFamily = Arabic, fontSize = if (z.arapca.length > 40) 22.sp else 31.sp,
                                    lineHeight = if (z.arapca.length > 40) 40.sp else 54.sp,
                                    color = p.text, textAlign = TextAlign.Center, textDirection = TextDirection.Rtl,
                                ),
                                maxLines = 3,
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
                                style = TextStyle(fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 15.sp, lineHeight = 19.sp, color = p.sub, textAlign = TextAlign.Center),
                                maxLines = 2,
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

    val prog = remember { Animatable(0f) }
    LaunchedEffect(shown, d.target, d.key) {
        val t = d.progress
        if (t < prog.value - 0.4f) prog.snapTo(0f)
        prog.animateTo(t, spring(stiffness = Spring.StiffnessLow))
    }
    val beadStep = 360f / 33f
    val rot = remember { Animatable(0f) }
    LaunchedEffect(d.count, d.key) {
        val t = d.count * beadStep
        if (abs(t - rot.value) > 400f) rot.snapTo(t) else rot.animateTo(t, spring(dampingRatio = 0.72f, stiffness = 340f))
    }
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(pulseKey) { if (pulseKey > 0) { pulse.snapTo(0f); pulse.animateTo(1f, tween(720, easing = FastOutSlowInEasing)) } }
    val burst = remember { Animatable(1f) }
    LaunchedEffect(celebrateKey) { if (celebrateKey > 0) { burst.snapTo(0f); burst.animateTo(1f, tween(1700, easing = LinearOutSlowInEasing)) } }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) { if (shakeKey > 0) for (x in listOf(9f, -7f, 5f, -3f, 0f)) shake.animateTo(x, tween(45)) }
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
            Canvas(Modifier.fillMaxSize()) { drawDome(p, prog.value, rot.value, pulse.value, burst.value, press) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val done = burst.value < 1f
                Text(
                    "TAMAMLANDI",
                    style = labelStyle(p.goldText).copy(fontSize = small, letterSpacing = 3.sp),
                    modifier = Modifier.graphicsLayer { alpha = if (done) sin(PI.toFloat() * burst.value).coerceIn(0f, 1f) else 0f },
                )
                AnimatedContent(
                    targetState = shown,
                    transitionSpec = {
                        val up = targetState > initialState
                        (slideInVertically(tween(260)) { if (up) it / 3 else -it / 3 } + fadeIn(tween(240))) togetherWith
                            (slideOutVertically(tween(200)) { if (up) -it / 3 else it / 3 } + fadeOut(tween(150))) using SizeTransform(clip = false)
                    },
                    label = "sayı",
                ) { v ->
                    Text(
                        "$v",
                        style = TextStyle(
                            brush = p.numberBrush, fontFamily = Display, fontWeight = FontWeight.Light,
                            fontSize = numSize, fontFeatureSettings = NUM_FEATURES,
                            shadow = if (p.dark) Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 4f), 18f) else null,
                        ),
                    )
                }
                Text(
                    "HEDEF  ${fmt(d.target)}",
                    style = labelStyle(p.sub).copy(fontSize = small, fontFeatureSettings = NUM_FEATURES),
                )
                if (d.laps > 0) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${d.laps}. TUR",
                        style = labelStyle(p.onGold).copy(fontWeight = FontWeight.Bold, fontFeatureSettings = NUM_FEATURES),
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(p.goldBrush).padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawDome(p: Pal, prog: Float, rot: Float, pulse: Float, burst: Float, press: Float) {
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
    for (i in 0 until 33) {
        val ang = -90f + i * stepDeg + rot
        var rel = ((ang + 90f) % 360f + 360f) % 360f
        if (rel > 180f) rel -= 360f
        val near = (1f - abs(rel) / stepDeg).coerceIn(0f, 1f)
        val pos = polar(rBeads, ang)
        drawCircle(Color.Black.copy(alpha = if (p.dark) 0.35f else 0.12f), beadR, Offset(pos.x, pos.y + beadR * 0.45f))
        if (near > 0f) drawCircle(p.goldLight.copy(alpha = 0.45f * near), beadR * 2.3f, pos)
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.95f), p.beadMid, p.beadEdge),
                center = Offset(pos.x - beadR * 0.35f, pos.y - beadR * 0.40f),
                radius = beadR * 1.35f,
            ),
            beadR, pos,
        )
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
    val facet = if (p.dark) Color.White.copy(alpha = 0.055f) else p.goldDeep.copy(alpha = 0.07f)
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
            listOf(Color(0xFFF3D9A4), Color(0xFFF2B8C6), Color(0xFFB9A8F0), Color(0xFF8EC3F2), Color(0xFFE9F1FF), Color(0xFFF3D9A4)),
            center = c,
        ),
        radius = rDisc, center = c, style = Stroke(1.7.dp.toPx() * k), alpha = if (p.dark) 0.9f else 0.75f,
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
    drawCircle(if (p.dark) Color.White.copy(alpha = 0.07f) else p.goldDeep.copy(alpha = 0.10f), rArc, c, style = Stroke(stroke))
    val sweep = 360f * prog
    if (sweep > 0.4f) {
        val tl = Offset(c.x - rArc, c.y - rArc)
        val sz = Size(rArc * 2, rArc * 2)
        rotate(-90f, c) {
            drawArc(p.gold.copy(alpha = 0.18f), 0f, sweep, false, tl, sz, style = Stroke(stroke * 3.2f, cap = StrokeCap.Round))
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
    val spark = polar(rDisc * 0.80f, -130f)
    drawCircle(Color.White.copy(alpha = 0.25f), 4.dp.toPx() * k, spark)
    drawCircle(Color.White.copy(alpha = 0.9f), 1.7.dp.toPx() * k, spark)

    // Hedef kutlaması: altın parçacıklar
    if (burst < 1f) {
        val a = 1f - burst
        drawCircle(p.goldLight.copy(alpha = a * 0.7f), rArc, c, style = Stroke(stroke * (1f + 2.5f * burst)))
        val e = 1f - (1f - burst) * (1f - burst)
        for (j in 0 until 28) {
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
        GlyphButton(G.UNDO, "Bir geri al (eksi bir)") { st.undo() }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (d.vird == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val presets = listOf(33, 99, 100)
                    presets.forEach { t -> GoldChip("$t", d.target == t, modifier = Modifier.widthIn(min = 52.dp)) { st.setTarget(t) } }
                    val custom = d.target !in presets
                    GoldChip(if (custom) fmt(d.target) else "Özel", custom, G.EDIT) { sheet(SheetKind.Target) }
                }
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
        Text(label, style = labelStyle(p.sub).copy(fontSize = 9.5.sp, fontFeatureSettings = NUM_FEATURES))
        Spacer(Modifier.height(2.dp))
        Text(value, style = TextStyle(brush = p.numberBrush, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, fontFeatureSettings = NUM_FEATURES))
    }
}
