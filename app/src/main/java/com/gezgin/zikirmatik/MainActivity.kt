package com.gezgin.zikirmatik

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

// ───────────── Renk paleti: gece zümrüdü ve altın ─────────────
private val Gold = Color(0xFFD9B46C)
private val GoldLight = Color(0xFFF6E7C1)
private val GoldDeep = Color(0xFF8E6B30)
private val Emerald = Color(0xFF1F6B57)
private val BgTop = Color(0xFF0E2A23)
private val BgBottom = Color(0xFF050E0B)
private val Disc = Color(0xFF0C231D)
private val Muted = Color(0xFF8FA89F)

data class Zikir(val arapca: String, val okunus: String, val anlam: String)

private val ZIKIRLER = listOf(
    Zikir("سُبْحَانَ اللّٰهِ", "Sübhânallah", "Allah her türlü noksanlıktan münezzehtir"),
    Zikir("اَلْحَمْدُ لِلّٰهِ", "Elhamdülillâh", "Hamd yalnızca Allah'adır"),
    Zikir("اَللّٰهُ اَكْبَرُ", "Allâhu Ekber", "Allah en büyüktür"),
    Zikir("لَا اِلٰهَ اِلَّا اللّٰهُ", "Lâ ilâhe illallah", "Allah'tan başka ilah yoktur"),
    Zikir("اَسْتَغْفِرُ اللّٰهَ", "Estağfirullah", "Allah'tan bağışlanma dilerim"),
    Zikir("اَللّٰهُمَّ صَلِّ عَلٰى مُحَمَّدٍ", "Salavât", "Allah'ım, Efendimiz Muhammed'e salât eyle"),
)

private val HEDEFLER = listOf(33, 99, 100, 1000)

// ───────────── Kalıcı kayıt ─────────────
private class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("zikirmatik", Context.MODE_PRIVATE)
    fun count(i: Int) = p.getInt("c$i", 0)
    fun setCount(i: Int, v: Int) = p.edit().putInt("c$i", v).apply()
    var selected: Int
        get() = p.getInt("sel", 0)
        set(v) = p.edit().putInt("sel", v).apply()
    var target: Int
        get() = p.getInt("target", 33)
        set(v) = p.edit().putInt("target", v).apply()
    var vibrate: Boolean
        get() = p.getBoolean("vib", true)
        set(v) = p.edit().putBoolean("vib", v).apply()
    var total: Int
        get() = p.getInt("total", 0)
        set(v) = p.edit().putInt("total", v).apply()
}

// ───────────── Titreşim ─────────────
private fun Context.vibrator(): Vibrator =
    if (Build.VERSION.SDK_INT >= 31) {
        getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

private fun Vibrator.tik(tamamlandi: Boolean) {
    if (!hasVibrator()) return
    if (tamamlandi) {
        vibrate(VibrationEffect.createWaveform(longArrayOf(0, 90, 70, 90, 70, 260), -1))
    } else {
        vibrate(VibrationEffect.createOneShot(16, 140))
    }
}

class MainActivity : ComponentActivity() {
    var onVolumeKey: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Gold, background = BgBottom, surface = Disc)) {
                ZikirmatikApp(this)
            }
        }
    }

    // Ses tuşlarıyla da zikir çekilebilsin (ekrana bakmadan)
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val action = onVolumeKey
            if (action != null) {
                if ((event?.repeatCount ?: 0) == 0) action()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun ZikirmatikApp(activity: MainActivity) {
    val ctx = LocalContext.current
    val store = remember { Store(ctx) }
    val vib = remember { ctx.vibrator() }
    val scope = rememberCoroutineScope()

    var sel by remember { mutableIntStateOf(store.selected.coerceIn(0, ZIKIRLER.lastIndex)) }
    val counts = remember { mutableStateListOf<Int>().apply { ZIKIRLER.indices.forEach { add(store.count(it)) } } }
    var target by remember { mutableIntStateOf(store.target) }
    var vibOn by remember { mutableStateOf(store.vibrate) }
    var total by remember { mutableIntStateOf(store.total) }
    var showReset by remember { mutableStateOf(false) }
    val pulse = remember { Animatable(1f) }

    val increment: () -> Unit = {
        val n = counts[sel] + 1
        counts[sel] = n
        store.setCount(sel, n)
        total += 1
        store.total = total
        val done = n % target == 0
        if (vibOn) vib.tik(done)
        scope.launch {
            pulse.snapTo(0f)
            pulse.animateTo(1f, tween(if (done) 1200 else 650, easing = FastOutSlowInEasing))
        }
    }
    val latestIncrement by rememberUpdatedState(increment)
    DisposableEffect(Unit) {
        activity.onVolumeKey = { latestIncrement() }
        onDispose { activity.onVolumeKey = null }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        PatternBackground()

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header(total)
            Spacer(Modifier.height(26.dp))
            ZikirSelector(sel) { sel = it; store.selected = it }
            Spacer(Modifier.weight(1f))
            CounterRing(count = counts[sel], target = target, pulse = pulse.value, onTap = increment)
            Spacer(Modifier.height(10.dp))
            Text(
                "Halkaya dokun ya da ses tuşlarını kullan",
                color = Muted.copy(alpha = 0.7f),
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
            )
            Spacer(Modifier.weight(1f))
            TargetRow(target) { target = it; store.target = it }
            Spacer(Modifier.height(16.dp))
            BottomBar(
                vibOn = vibOn,
                onVib = { vibOn = !vibOn; store.vibrate = vibOn; if (vibOn) vib.tik(false) },
                onReset = { showReset = true },
            )
        }

        if (showReset) {
            AlertDialog(
                onDismissRequest = { showReset = false },
                containerColor = Color(0xFF10291F),
                shape = RoundedCornerShape(28.dp),
                title = { Text("Sayacı sıfırla", color = GoldLight, fontWeight = FontWeight.Medium) },
                text = {
                    Text(
                        "${ZIKIRLER[sel].okunus} sayacı (${counts[sel]}) sıfırlansın mı?",
                        color = Muted,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        counts[sel] = 0
                        store.setCount(sel, 0)
                        showReset = false
                    }) { Text("Sıfırla", color = Gold, fontWeight = FontWeight.SemiBold) }
                },
                dismissButton = {
                    TextButton(onClick = { showReset = false }) { Text("Vazgeç", color = Muted) }
                },
            )
        }
    }
}

@Composable
private fun Header(total: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                "ZİKİRMATİK",
                color = Gold,
                fontSize = 13.sp,
                letterSpacing = 6.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Kalpler ancak Allah'ı anmakla huzur bulur",
                color = Muted,
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("TOPLAM", color = Muted, fontSize = 9.sp, letterSpacing = 3.sp)
            Text(
                String.format(Locale.forLanguageTag("tr"), "%,d", total),
                color = GoldLight,
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
            )
        }
    }
}

@Composable
private fun ZikirSelector(sel: Int, onChange: (Int) -> Unit) {
    val n = ZIKIRLER.size
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.AutoMirrored.Filled.KeyboardArrowLeft) { onChange((sel - 1 + n) % n) }
            AnimatedContent(
                targetState = sel,
                modifier = Modifier.weight(1f),
                transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) },
                label = "zikir",
            ) { i ->
                val z = ZIKIRLER[i]
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        z.arapca,
                        color = GoldLight,
                        fontSize = 30.sp,
                        lineHeight = 48.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(textDirection = TextDirection.Rtl),
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        z.okunus,
                        color = Gold,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(z.anlam, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
            RoundIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight) { onChange((sel + 1) % n) }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ZIKIRLER.indices.forEach { i ->
                val w by animateDpAsState(if (i == sel) 20.dp else 6.dp, label = "dot")
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .width(w)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(if (i == sel) Gold else Gold.copy(alpha = 0.22f))
                        .clickable { onChange(i) }
                )
            }
        }
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .border(1.dp, Gold.copy(alpha = 0.28f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Gold)
    }
}

@Composable
private fun CounterRing(count: Int, target: Int, pulse: Float, onTap: () -> Unit) {
    val shown = if (count > 0 && count % target == 0) target else count % target
    val laps = count / target

    val prog = remember { Animatable(0f) }
    LaunchedEffect(shown, target) {
        val t = shown / target.toFloat()
        if (t < prog.value - 0.5f) prog.snapTo(0f) // yeni tura geçişte geri sarmasın
        prog.animateTo(t, spring(stiffness = Spring.StiffnessLow))
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.95f else 1f,
        spring(dampingRatio = 0.45f, stiffness = 700f),
        label = "press",
    )

    Box(
        Modifier
            .size(310.dp)
            .scale(scale)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) { drawRing(prog.value, pulse, target) }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(
                targetState = shown,
                transitionSpec = {
                    (slideInVertically { it / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutVertically { -it / 3 } + fadeOut(tween(160))) using
                        SizeTransform(clip = false)
                },
                label = "sayi",
            ) { v ->
                Text(
                    "$v",
                    color = GoldLight,
                    fontSize = 86.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Serif,
                )
            }
            Text("/ $target", color = Muted, fontSize = 15.sp, letterSpacing = 2.sp)
            if (laps > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "$laps TUR",
                    color = Gold,
                    fontSize = 10.sp,
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

private fun DrawScope.drawRing(p: Float, pulse: Float, target: Int) {
    val c = center
    val rOuter = size.minDimension / 2 - 10.dp.toPx()
    val rArc = rOuter - 22.dp.toPx()
    val stroke = 6.dp.toPx()

    // Yumuşak zümrüt hale
    drawCircle(
        Brush.radialGradient(listOf(Emerald.copy(alpha = 0.40f), Color.Transparent), center = c, radius = rOuter + 10.dp.toPx()),
        radius = rOuter + 10.dp.toPx(), center = c,
    )
    // İç disk
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFF14423A), Disc), center = Offset(c.x, c.y - rArc * 0.3f), radius = rArc * 1.2f),
        radius = rArc - stroke * 1.4f, center = c,
    )
    drawCircle(Gold.copy(alpha = 0.16f), radius = rArc - stroke * 2.6f, center = c, style = Stroke(1.dp.toPx()))

    // Dokunuş dalgası
    if (pulse < 1f) {
        val a = 1f - pulse
        drawCircle(
            GoldLight.copy(alpha = a * 0.45f),
            radius = rArc + (rOuter - rArc + 18.dp.toPx()) * pulse,
            center = c,
            style = Stroke(2.5.dp.toPx() * a + 0.5f),
        )
    }

    // Tespih taneleri
    val beads = if (target <= 100) target else 100
    val filled = floor(p * beads + 0.001f).toInt()
    val beadR = when {
        beads <= 33 -> 3.4.dp.toPx()
        beads <= 99 -> 2.1.dp.toPx()
        else -> 1.9.dp.toPx()
    }
    for (i in 0 until beads) {
        val ang = Math.toRadians(-90.0 + 360.0 * i / beads)
        val pos = Offset(c.x + rOuter * cos(ang).toFloat(), c.y + rOuter * sin(ang).toFloat())
        drawCircle(if (i < filled) Gold else Gold.copy(alpha = 0.16f), radius = beadR, center = pos)
    }

    // Halka izi + ilerleme yayı
    drawCircle(Gold.copy(alpha = 0.10f), radius = rArc, center = c, style = Stroke(stroke))
    val sweep = 360f * p
    if (sweep > 0.5f) {
        rotate(-90f, c) {
            drawArc(
                brush = Brush.sweepGradient(
                    0f to GoldDeep, 0.5f to Gold, 1f to GoldLight,
                    center = c,
                ),
                startAngle = 0f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(c.x - rArc, c.y - rArc),
                size = Size(rArc * 2, rArc * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        val ang = Math.toRadians(-90.0 + sweep)
        val head = Offset(c.x + rArc * cos(ang).toFloat(), c.y + rArc * sin(ang).toFloat())
        drawCircle(GoldLight.copy(alpha = 0.28f), radius = stroke * 1.9f, center = head)
        drawCircle(GoldLight, radius = stroke * 0.7f, center = head)
    }
}

@Composable
private fun TargetRow(target: Int, onSelect: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("HEDEF", color = Muted, fontSize = 9.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HEDEFLER.forEach { t ->
                val on = t == target
                val bg by animateColorAsState(if (on) Gold else Color.Transparent, label = "bg")
                val fg by animateColorAsState(if (on) BgBottom else Gold, label = "fg")
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(bg)
                        .border(1.dp, Gold.copy(alpha = if (on) 1f else 0.32f), RoundedCornerShape(50))
                        .clickable { onSelect(t) }
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                ) {
                    Text("$t", color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun BottomBar(vibOn: Boolean, onVib: () -> Unit, onReset: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Pill(onClick = onVib) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (vibOn) Gold else Muted.copy(alpha = 0.4f))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (vibOn) "TİTREŞİM AÇIK" else "TİTREŞİM KAPALI",
                color = if (vibOn) Gold else Muted,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
            )
        }
        Pill(onClick = onReset) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = Gold, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text("SIFIRLA", color = Gold, fontSize = 10.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun Pill(onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Gold.copy(alpha = 0.2f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

// Arka planda çok silik, tekrar eden sekiz köşeli yıldız deseni
@Composable
private fun PatternBackground() {
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(
            Brush.radialGradient(
                listOf(Emerald.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(size.width / 2, size.height * 0.55f),
                radius = size.width,
            ),
            radius = size.width,
            center = Offset(size.width / 2, size.height * 0.55f),
        )
        val tile = 64.dp.toPx()
        val half = tile * 0.22f
        val col = Gold.copy(alpha = 0.045f)
        val st = Stroke(1.dp.toPx())
        var y = 0f
        var row = 0
        while (y < size.height + tile) {
            var x = if (row % 2 == 0) 0f else tile / 2
            while (x < size.width + tile) {
                val o = Offset(x, y)
                val tl = Offset(x - half, y - half)
                val sz = Size(half * 2, half * 2)
                drawRect(col, topLeft = tl, size = sz, style = st)
                rotate(45f, o) { drawRect(col, topLeft = tl, size = sz, style = st) }
                drawCircle(col, radius = half * 0.35f, center = o, style = st)
                x += tile
            }
            y += tile * 0.8f
            row++
        }
    }
}
