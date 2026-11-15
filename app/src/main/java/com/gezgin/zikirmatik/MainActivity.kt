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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

// ───────────── Palet: inci, kristal ve şampanya altını ─────────────
private val InkSoft = Color(0xFF3A342B)
private val Gold = Color(0xFFB8913F)
private val GoldLight = Color(0xFFE9CF8E)
private val GoldDeep = Color(0xFF8A6A2C)
private val Muted = Color(0xFF8C8478)
private val GoldBrush = Brush.linearGradient(listOf(GoldDeep, Gold, GoldLight, Gold, GoldDeep))
private val Serif = FontFamily.Serif

data class Zikir(
    val id: Long,
    val arapca: String,
    val okunus: String,
    val anlam: String,
    val count: Int = 0,
    val target: Int = 33,
)

private val VARSAYILAN = listOf(
    Zikir(1, "سُبْحَانَ اللّٰهِ", "Sübhânallah", "Allah her türlü noksanlıktan münezzehtir"),
    Zikir(2, "اَلْحَمْدُ لِلّٰهِ", "Elhamdülillâh", "Hamd yalnızca Allah'adır"),
    Zikir(3, "اَللّٰهُ اَكْبَرُ", "Allâhu Ekber", "Allah en büyüktür"),
    Zikir(4, "لَا اِلٰهَ اِلَّا اللّٰهُ", "Lâ ilâhe illallah", "Allah'tan başka ilah yoktur", target = 100),
    Zikir(5, "اَسْتَغْفِرُ اللّٰهَ", "Estağfirullah", "Allah'tan bağışlanma dilerim", target = 100),
    Zikir(6, "اَللّٰهُمَّ صَلِّ عَلٰى مُحَمَّدٍ", "Salavât", "Allah'ım, Efendimiz Muhammed'e salât eyle", target = 100),
)

private val HAZIR_HEDEFLER = listOf(33, 99, 100)

// ───────────── Kalıcı kayıt ─────────────
private class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("zikirmatik", Context.MODE_PRIVATE)

    fun load(): List<Zikir> {
        val s = p.getString("list", null)
        if (s != null) {
            runCatching {
                val a = JSONArray(s)
                val out = (0 until a.length()).map { i ->
                    val o = a.getJSONObject(i)
                    Zikir(
                        id = o.getLong("id"),
                        arapca = o.optString("ar"),
                        okunus = o.getString("ok"),
                        anlam = o.optString("an"),
                        count = o.optInt("c", 0),
                        target = o.optInt("t", 33).coerceAtLeast(1),
                    )
                }
                if (out.isNotEmpty()) return out
            }
        }
        return VARSAYILAN
    }

    fun save(list: List<Zikir>) {
        val a = JSONArray()
        list.forEach {
            a.put(
                JSONObject()
                    .put("id", it.id).put("ar", it.arapca).put("ok", it.okunus)
                    .put("an", it.anlam).put("c", it.count).put("t", it.target)
            )
        }
        p.edit().putString("list", a.toString()).apply()
    }

    var selectedId: Long
        get() = p.getLong("selId", 1L)
        set(v) = p.edit().putLong("selId", v).apply()
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
    if (tamamlandi) vibrate(VibrationEffect.createWaveform(longArrayOf(0, 90, 70, 90, 70, 260), -1))
    else vibrate(VibrationEffect.createOneShot(16, 140))
}

private sealed interface Dlg {
    data object Reset : Dlg
    data object Liste : Dlg
    data object Hedef : Dlg
    data class Duzenle(val zikir: Zikir?) : Dlg
    data class Sil(val zikir: Zikir) : Dlg
}

class MainActivity : ComponentActivity() {
    var onVolumeKey: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Gold, background = Color(0xFFF7F3EC), surface = Color.White)) {
                ZikirmatikApp(this)
            }
        }
    }

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

    val list = remember { mutableStateListOf<Zikir>().apply { addAll(store.load()) } }
    var selId by remember { mutableLongStateOf(store.selectedId) }
    var vibOn by remember { mutableStateOf(store.vibrate) }
    var total by remember { mutableIntStateOf(store.total) }
    var dialog by remember { mutableStateOf<Dlg?>(null) }
    val pulse = remember { Animatable(1f) }

    val idx = list.indexOfFirst { it.id == selId }.let { if (it < 0) 0 else it }
    val cur = list[idx]

    fun select(id: Long) { selId = id; store.selectedId = id }
    fun replace(z: Zikir) {
        val i = list.indexOfFirst { it.id == z.id }
        if (i >= 0) { list[i] = z; store.save(list) }
    }

    val increment: () -> Unit = {
        if (dialog == null) {
            val i = list.indexOfFirst { it.id == selId }.let { if (it < 0) 0 else it }
            val z = list[i]
            val n = z.count + 1
            list[i] = z.copy(count = n)
            store.save(list)
            total += 1
            store.total = total
            val done = n % z.target == 0
            if (vibOn) vib.tik(done)
            scope.launch {
                pulse.snapTo(0f)
                pulse.animateTo(1f, tween(if (done) 1300 else 700, easing = FastOutSlowInEasing))
            }
        }
    }
    val latestIncrement by rememberUpdatedState(increment)
    DisposableEffect(Unit) {
        activity.onVolumeKey = { latestIncrement() }
        onDispose { activity.onVolumeKey = null }
    }

    Box(Modifier.fillMaxSize()) {
        PearlBackground()

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header(onList = { dialog = Dlg.Liste }, onAdd = { dialog = Dlg.Duzenle(null) })
            Spacer(Modifier.height(18.dp))
            ZikirSelector(list, idx) { select(list[it].id) }
            Spacer(Modifier.weight(1f))
            CounterRing(count = cur.count, target = cur.target, pulse = pulse.value, onTap = increment)
            Spacer(Modifier.height(8.dp))
            Text(
                "Kristale dokun ya da ses tuşlarını kullan",
                color = Muted.copy(alpha = 0.8f),
                fontSize = 11.sp,
                letterSpacing = 0.4.sp,
            )
            Spacer(Modifier.weight(1f))
            TargetRow(
                target = cur.target,
                onPreset = { replace(cur.copy(target = it)) },
                onCustom = { dialog = Dlg.Hedef },
            )
            Spacer(Modifier.height(16.dp))
            BottomBar(
                vibOn = vibOn,
                total = total,
                onVib = { vibOn = !vibOn; store.vibrate = vibOn; if (vibOn) vib.tik(false) },
                onReset = { dialog = Dlg.Reset },
            )
        }

        when (val d = dialog) {
            null -> Unit
            Dlg.Reset -> ConfirmDialog(
                title = "Sayacı Sıfırla",
                message = "${cur.okunus} sayacı (${cur.count}) sıfırlansın mı?",
                confirm = "Sıfırla",
                onConfirm = { replace(cur.copy(count = 0)); dialog = null },
                onDismiss = { dialog = null },
            )
            Dlg.Hedef -> TargetDialog(
                initial = cur.target,
                onSave = { replace(cur.copy(target = it)); dialog = null },
                onDismiss = { dialog = null },
            )
            Dlg.Liste -> ListDialog(
                list = list,
                selId = cur.id,
                onSelect = { select(it.id); dialog = null },
                onEdit = { dialog = Dlg.Duzenle(it) },
                onDelete = { dialog = Dlg.Sil(it) },
                onAdd = { dialog = Dlg.Duzenle(null) },
                onDismiss = { dialog = null },
            )
            is Dlg.Duzenle -> EditDialog(
                initial = d.zikir,
                onSave = { ok, ar, an, t ->
                    val z = d.zikir
                    if (z == null) {
                        val nz = Zikir(System.currentTimeMillis(), ar, ok, an, 0, t)
                        list.add(nz)
                        store.save(list)
                        select(nz.id)
                    } else {
                        replace(z.copy(okunus = ok, arapca = ar, anlam = an, target = t))
                    }
                    dialog = null
                },
                onDismiss = { dialog = if (d.zikir != null) Dlg.Liste else null },
            )
            is Dlg.Sil -> ConfirmDialog(
                title = "Zikri Sil",
                message = "“${d.zikir.okunus}” listeden silinsin mi? Sayacı da silinir.",
                confirm = "Sil",
                onConfirm = {
                    list.removeAll { it.id == d.zikir.id }
                    store.save(list)
                    if (selId == d.zikir.id) select(list.first().id)
                    dialog = Dlg.Liste
                },
                onDismiss = { dialog = Dlg.Liste },
            )
        }
    }
}

// ───────────── Cam (glass) yüzey ─────────────
private fun Modifier.glass(shape: Shape, strong: Boolean = false): Modifier = this
    .clip(shape)
    .background(
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = if (strong) 0.80f else 0.58f),
                Color.White.copy(alpha = if (strong) 0.55f else 0.24f),
            ),
        ),
        shape,
    )
    .border(
        1.dp,
        Brush.linearGradient(
            listOf(Color.White, Color.White.copy(alpha = 0.35f), GoldLight.copy(alpha = 0.7f)),
        ),
        shape,
    )

// İnci zemin + yumuşak, dağılmış ışık lekeleri (şampanya, lila, gök mavisi)
@Composable
private fun PearlBackground() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFFFDFBF7), Color(0xFFF4EFE6), Color(0xFFEDE7DC))))
        fun glow(color: Color, x: Float, y: Float, r: Float) {
            val o = Offset(size.width * x, size.height * y)
            val rr = size.width * r
            drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center = o, radius = rr), radius = rr, center = o)
        }
        glow(Color(0xFFF3DDAA).copy(alpha = 0.55f), 0.85f, 0.10f, 0.75f)
        glow(Color(0xFFE3DAF5).copy(alpha = 0.60f), 0.05f, 0.38f, 0.70f)
        glow(Color(0xFFD7E6F6).copy(alpha = 0.60f), 0.95f, 0.70f, 0.75f)
        glow(Color(0xFFF6E0D6).copy(alpha = 0.55f), 0.15f, 0.95f, 0.70f)
        glow(Color.White.copy(alpha = 0.85f), 0.5f, 0.52f, 0.55f)
    }
}

@Composable
private fun Ornament(width: Dp = 150.dp) {
    Canvas(Modifier.width(width).height(10.dp)) {
        val cy = size.height / 2
        val cx = size.width / 2
        val gap = 12.dp.toPx()
        val sw = 0.8.dp.toPx()
        drawLine(
            Brush.horizontalGradient(listOf(Color.Transparent, Gold.copy(alpha = 0.7f)), startX = 0f, endX = cx - gap),
            Offset(0f, cy), Offset(cx - gap, cy), sw,
        )
        drawLine(
            Brush.horizontalGradient(listOf(Gold.copy(alpha = 0.7f), Color.Transparent), startX = cx + gap, endX = size.width),
            Offset(cx + gap, cy), Offset(size.width, cy), sw,
        )
        val d = 4.dp.toPx()
        val diamond = Path().apply {
            moveTo(cx, cy - d); lineTo(cx + d, cy); lineTo(cx, cy + d); lineTo(cx - d, cy); close()
        }
        drawPath(diamond, GoldBrush)
        drawCircle(Gold.copy(alpha = 0.7f), 1.2.dp.toPx(), Offset(cx - gap + 4.dp.toPx(), cy))
        drawCircle(Gold.copy(alpha = 0.7f), 1.2.dp.toPx(), Offset(cx + gap - 4.dp.toPx(), cy))
    }
}

@Composable
private fun Header(onList: () -> Unit, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RoundIcon(Icons.Filled.Menu, onClick = onList)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "ZİKİRMATİK",
                style = TextStyle(
                    brush = GoldBrush, fontSize = 16.sp, letterSpacing = 7.sp,
                    fontWeight = FontWeight.Medium, fontFamily = Serif,
                ),
            )
            Spacer(Modifier.height(5.dp))
            Ornament(110.dp)
            Spacer(Modifier.height(5.dp))
            Text(
                "Kalpler ancak Allah'ı anmakla huzur bulur",
                color = Muted, fontSize = 10.5.sp, fontStyle = FontStyle.Italic,
            )
        }
        RoundIcon(Icons.Filled.Add, onClick = onAdd)
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, dim: Dp = 42.dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(dim)
            .glass(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(dim * 0.48f))
    }
}

@Composable
private fun ZikirSelector(list: List<Zikir>, idx: Int, onChange: (Int) -> Unit) {
    val n = list.size
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(26.dp))
                .padding(horizontal = 8.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (n > 1) ArrowTap(Icons.AutoMirrored.Filled.KeyboardArrowLeft) { onChange((idx - 1 + n) % n) }
            AnimatedContent(
                targetState = list[idx],
                contentKey = { it.id },
                modifier = Modifier.weight(1f),
                transitionSpec = { fadeIn(tween(380)) togetherWith fadeOut(tween(220)) },
                label = "zikir",
            ) { z ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (z.arapca.isNotBlank()) {
                        Text(
                            z.arapca,
                            color = InkSoft,
                            fontSize = 28.sp,
                            lineHeight = 44.sp,
                            textAlign = TextAlign.Center,
                            style = TextStyle(textDirection = TextDirection.Rtl),
                            maxLines = 2,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            z.okunus, color = Gold, fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Text(
                            z.okunus,
                            style = TextStyle(brush = GoldBrush, fontSize = 26.sp, fontFamily = Serif),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                    if (z.anlam.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(z.anlam, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2)
                    }
                }
            }
            if (n > 1) ArrowTap(Icons.AutoMirrored.Filled.KeyboardArrowRight) { onChange((idx + 1) % n) }
        }
        Spacer(Modifier.height(12.dp))
        if (n in 2..12) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                list.indices.forEach { i ->
                    val w by animateDpAsState(if (i == idx) 20.dp else 6.dp, label = "dot")
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .width(w)
                            .height(6.dp)
                            .clip(CircleShape)
                            .then(
                                if (i == idx) Modifier.background(GoldBrush)
                                else Modifier.background(Color.White.copy(alpha = 0.9f)).border(0.6.dp, Gold.copy(alpha = 0.35f), CircleShape)
                            )
                            .clickable { onChange(i) }
                    )
                }
            }
        } else if (n > 12) {
            Text("${idx + 1} / $n", color = Muted, fontSize = 11.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun ArrowTap(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Gold.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun CounterRing(count: Int, target: Int, pulse: Float, onTap: () -> Unit) {
    val shown = if (count > 0 && count % target == 0) target else count % target
    val laps = count / target

    val prog = remember { Animatable(0f) }
    LaunchedEffect(shown, target) {
        val t = shown / target.toFloat()
        if (t < prog.value - 0.5f) prog.snapTo(0f)
        prog.animateTo(t, spring(stiffness = Spring.StiffnessLow))
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "press",
    )

    Box(
        Modifier
            .size(300.dp)
            .scale(scale)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) { drawCrystal(prog.value, pulse) }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(
                targetState = shown,
                transitionSpec = {
                    (slideInVertically { it / 3 } + fadeIn(tween(240))) togetherWith
                        (slideOutVertically { -it / 3 } + fadeOut(tween(160))) using
                        SizeTransform(clip = false)
                },
                label = "sayi",
            ) { v ->
                Text(
                    "$v",
                    style = TextStyle(
                        brush = GoldBrush, fontSize = if (v >= 1000) 64.sp else 82.sp,
                        fontWeight = FontWeight.Light, fontFamily = Serif,
                    ),
                )
            }
            Text("/ $target", color = Muted, fontSize = 14.sp, letterSpacing = 2.sp)
            if (laps > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "$laps TUR",
                    color = Gold,
                    fontSize = 10.sp,
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .glass(RoundedCornerShape(50), strong = true)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

// Kristal disk: prizma ışıltılı kenar, kesme yüzeyler, parıltı ve altın ilerleme
private fun DrawScope.drawCrystal(p: Float, pulse: Float) {
    val c = center
    val rOuter = size.minDimension / 2 - 6.dp.toPx()
    val major = 10.dp.toPx()
    val minor = 4.5.dp.toPx()
    val rDisc = rOuter - major - 8.dp.toPx()
    val rArc = rDisc - 12.dp.toPx()
    val stroke = 3.dp.toPx()

    fun polar(r: Float, deg: Double): Offset {
        val a = Math.toRadians(deg)
        return Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
    }

    // Yumuşak altın gölge / ışık hâlesi
    val shadowC = Offset(c.x, c.y + 14.dp.toPx())
    drawCircle(
        Brush.radialGradient(listOf(Gold.copy(alpha = 0.16f), Color.Transparent), center = shadowC, radius = rOuter * 1.1f),
        radius = rOuter * 1.1f, center = shadowC,
    )

    // Dış cam halka (kadran)
    drawCircle(
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0.25f)),
            start = Offset(c.x - rOuter, c.y - rOuter), end = Offset(c.x + rOuter, c.y + rOuter),
        ),
        radius = rOuter, center = c,
    )
    // Prizma kenarı
    drawCircle(
        Brush.sweepGradient(
            listOf(
                Color(0xFFF2D9A0), Color(0xFFF4D3DA), Color(0xFFDCD4F3),
                Color(0xFFCFE2F4), Color(0xFFF7F1DF), Color(0xFFF2D9A0),
            ),
            center = c,
        ),
        radius = rOuter, center = c, style = Stroke(2.dp.toPx()),
    )
    drawCircle(Color.White, radius = rOuter - 1.5.dp.toPx(), center = c, style = Stroke(0.8.dp.toPx()))

    // Kertikler
    val n = 60
    val filled = floor(p * n + 0.001f).toInt()
    for (i in 0 until n) {
        val isMajor = i % 5 == 0
        val deg = -90.0 + 6.0 * i
        val r1 = rOuter - 5.dp.toPx()
        val r2 = r1 - if (isMajor) major else minor
        val col = if (i < filled) {
            if (isMajor) GoldDeep else Gold
        } else {
            InkSoft.copy(alpha = if (isMajor) 0.22f else 0.10f)
        }
        drawLine(col, polar(r1, deg), polar(r2, deg), if (isMajor) 1.8.dp.toPx() else 1.dp.toPx(), cap = StrokeCap.Round)
    }

    // Kristal disk gövdesi
    drawCircle(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.95f), Color(0xFFF7F3EC).copy(alpha = 0.75f), Color(0xFFEDE6F3).copy(alpha = 0.6f)),
            center = Offset(c.x - rDisc * 0.25f, c.y - rDisc * 0.3f),
            radius = rDisc * 1.4f,
        ),
        radius = rDisc, center = c,
    )
    // Kesme yüzeyler (sekizgen faset çizgileri)
    val facet = Gold.copy(alpha = 0.07f)
    val rf = rDisc * 0.62f
    val oct = Path().apply {
        for (k in 0..7) {
            val pt = polar(rf, -90.0 + 22.5 + 45.0 * k)
            if (k == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
        }
        close()
    }
    drawPath(oct, facet, style = Stroke(1.dp.toPx()))
    for (k in 0..7) {
        val deg = -90.0 + 22.5 + 45.0 * k
        drawLine(facet, polar(rf, deg), polar(rDisc, deg), 1.dp.toPx())
    }
    drawCircle(
        Brush.linearGradient(
            listOf(Color.White, GoldLight.copy(alpha = 0.6f), Color.White.copy(alpha = 0.4f)),
            start = Offset(c.x - rDisc, c.y - rDisc), end = Offset(c.x + rDisc, c.y + rDisc),
        ),
        radius = rDisc, center = c, style = Stroke(1.2.dp.toPx()),
    )

    // Dokunuş dalgası
    if (pulse < 1f) {
        val a = 1f - pulse
        drawCircle(
            Gold.copy(alpha = a * 0.45f),
            radius = rArc + (rOuter - rArc + 18.dp.toPx()) * pulse,
            center = c,
            style = Stroke(2.dp.toPx() * a + 0.5f),
        )
        drawCircle(Color.White.copy(alpha = a * 0.5f), radius = rDisc * (0.4f + 0.6f * pulse), center = c)
    }

    // İlerleme yayı
    drawCircle(InkSoft.copy(alpha = 0.06f), radius = rArc, center = c, style = Stroke(stroke))
    val sweep = 360f * p
    if (sweep > 0.5f) {
        rotate(-90f, c) {
            drawArc(
                brush = Brush.sweepGradient(0f to GoldLight, 0.5f to Gold, 1f to GoldDeep, center = c),
                startAngle = 0f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(c.x - rArc, c.y - rArc),
                size = Size(rArc * 2, rArc * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        val head = polar(rArc, -90.0 + sweep)
        drawCircle(GoldLight.copy(alpha = 0.55f), radius = stroke * 2.6f, center = head)
        drawCircle(Color.White, radius = stroke * 1.1f, center = head)
        drawCircle(Gold, radius = stroke * 1.1f, center = head, style = Stroke(1.dp.toPx()))
    }

    // Parıltı (sol üstte cam yansıması)
    rotate(-35f, c) {
        drawOval(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0f)),
                startY = c.y - rDisc * 0.92f, endY = c.y - rDisc * 0.45f,
            ),
            topLeft = Offset(c.x - rDisc * 0.55f, c.y - rDisc * 0.92f),
            size = Size(rDisc * 1.1f, rDisc * 0.5f),
        )
    }
    drawCircle(Color.White.copy(alpha = 0.9f), radius = 2.dp.toPx(), center = polar(rDisc * 0.78f, -128.0))
}

@Composable
private fun TargetRow(target: Int, onPreset: (Int) -> Unit, onCustom: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("HEDEF", color = Muted, fontSize = 9.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            HAZIR_HEDEFLER.forEach { t -> GoldChip("$t", t == target) { onPreset(t) } }
            val custom = target !in HAZIR_HEDEFLER
            GoldChip(if (custom) "$target" else "ÖZEL", custom, Icons.Filled.Edit, onCustom)
        }
    }
}

@Composable
private fun GoldChip(label: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .then(
                if (selected) Modifier.clip(shape).background(GoldBrush).border(1.dp, GoldLight, shape)
                else Modifier.glass(shape)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (selected) Color.White else Gold, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            label,
            color = if (selected) Color.White else GoldDeep,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun BottomBar(vibOn: Boolean, total: Int, onVib: () -> Unit, onReset: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Pill(onClick = onVib) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (vibOn) Gold else Muted.copy(alpha = 0.35f))
            )
            Spacer(Modifier.width(7.dp))
            Text("TİTREŞİM", color = if (vibOn) GoldDeep else Muted, fontSize = 9.5.sp, letterSpacing = 2.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TOPLAM", color = Muted, fontSize = 8.5.sp, letterSpacing = 3.sp)
            Text(
                String.format(Locale.forLanguageTag("tr"), "%,d", total),
                style = TextStyle(brush = GoldBrush, fontSize = 17.sp, fontFamily = Serif, fontWeight = FontWeight.Medium),
            )
        }
        Pill(onClick = onReset) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = Gold, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("SIFIRLA", color = GoldDeep, fontSize = 9.5.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun Pill(onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

// ───────────── Diyaloglar ─────────────
@Composable
private fun LuxDialog(
    title: String,
    onDismiss: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val shape = RoundedCornerShape(26.dp)
        Column(
            Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFFFFFEFB), Color(0xFFF6F1E8))))
                .border(
                    1.dp,
                    Brush.linearGradient(listOf(Color.White, GoldLight, Color.White)),
                    shape,
                )
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = TextStyle(brush = GoldBrush, fontSize = 19.sp, fontFamily = Serif, letterSpacing = 1.sp, fontWeight = FontWeight.Medium))
            Spacer(Modifier.height(10.dp))
            Ornament(110.dp)
            Spacer(Modifier.height(18.dp))
            content()
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

@Composable
private fun GoldButton(text: String, primary: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .then(if (primary && enabled) Modifier.background(GoldBrush) else Modifier)
            .border(1.dp, Gold.copy(alpha = if (enabled) 0.55f else 0.2f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(
            text,
            color = when {
                !enabled -> Muted.copy(alpha = 0.5f)
                primary -> Color.White
                else -> GoldDeep
            },
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun GoldField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    number: Boolean = false,
    rtl: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        textStyle = TextStyle(
            color = InkSoft,
            fontSize = if (rtl) 20.sp else 16.sp,
            textDirection = if (rtl) TextDirection.Rtl else TextDirection.Content,
        ),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold,
            unfocusedBorderColor = Gold.copy(alpha = 0.3f),
            focusedLabelColor = Gold,
            unfocusedLabelColor = Muted,
            cursorColor = Gold,
            focusedTextColor = InkSoft,
            unfocusedTextColor = InkSoft,
            focusedContainerColor = Color.White.copy(alpha = 0.7f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.5f),
        ),
    )
}

@Composable
private fun ConfirmDialog(title: String, message: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    LuxDialog(
        title = title,
        onDismiss = onDismiss,
        actions = {
            GoldButton("Vazgeç", primary = false, onClick = onDismiss)
            GoldButton(confirm, primary = true, onClick = onConfirm)
        },
    ) {
        Text(message, color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TargetDialog(initial: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial.toString()) }
    val value = text.toIntOrNull()
    val valid = value != null && value in 1..1_000_000
    LuxDialog(
        title = "Hedef Belirle",
        onDismiss = onDismiss,
        actions = {
            GoldButton("Vazgeç", primary = false, onClick = onDismiss)
            GoldButton("Kaydet", primary = true, enabled = valid) { onSave(value!!) }
        },
    ) {
        GoldField(text, { s -> text = s.filter { it.isDigit() }.take(7) }, "Hedef sayı", number = true)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(7, 41, 500, 1000).forEach { t ->
                GoldChip("$t", text == t.toString()) { text = t.toString() }
            }
        }
    }
}

@Composable
private fun EditDialog(
    initial: Zikir?,
    onSave: (okunus: String, arapca: String, anlam: String, target: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var okunus by remember { mutableStateOf(initial?.okunus ?: "") }
    var arapca by remember { mutableStateOf(initial?.arapca ?: "") }
    var anlam by remember { mutableStateOf(initial?.anlam ?: "") }
    var hedef by remember { mutableStateOf((initial?.target ?: 33).toString()) }
    val t = hedef.toIntOrNull()
    val valid = okunus.isNotBlank() && t != null && t in 1..1_000_000

    LuxDialog(
        title = if (initial == null) "Yeni Zikir" else "Zikri Düzenle",
        onDismiss = onDismiss,
        actions = {
            GoldButton("Vazgeç", primary = false, onClick = onDismiss)
            GoldButton("Kaydet", primary = true, enabled = valid) {
                onSave(okunus.trim(), arapca.trim(), anlam.trim(), t!!)
            }
        },
    ) {
        Column(
            Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GoldField(okunus, { okunus = it.take(60) }, "Zikir adı / okunuşu *")
            GoldField(arapca, { arapca = it.take(80) }, "Arapça yazılışı (isteğe bağlı)", rtl = true)
            GoldField(anlam, { anlam = it.take(120) }, "Anlamı (isteğe bağlı)")
            GoldField(hedef, { s -> hedef = s.filter { it.isDigit() }.take(7) }, "Hedef", number = true)
        }
    }
}

@Composable
private fun ListDialog(
    list: List<Zikir>,
    selId: Long,
    onSelect: (Zikir) -> Unit,
    onEdit: (Zikir) -> Unit,
    onDelete: (Zikir) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    LuxDialog(
        title = "Zikirlerim",
        onDismiss = onDismiss,
        actions = {
            GoldButton("Kapat", primary = false, onClick = onDismiss)
            GoldButton("+ Yeni Zikir", primary = true, onClick = onAdd)
        },
    ) {
        LazyColumn(Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.id }) { z ->
                val on = z.id == selId
                val shape = RoundedCornerShape(16.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(if (on) GoldLight.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.7f))
                        .border(1.dp, Gold.copy(alpha = if (on) 0.6f else 0.18f), shape)
                        .clickable { onSelect(z) }
                        .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            z.okunus, color = if (on) GoldDeep else InkSoft, fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${z.count} · hedef ${z.target}",
                            color = Muted, fontSize = 11.sp, letterSpacing = 0.5.sp,
                        )
                    }
                    IconTap(Icons.Filled.Edit) { onEdit(z) }
                    if (list.size > 1) IconTap(Icons.Filled.Delete) { onDelete(z) }
                }
            }
        }
    }
}

@Composable
private fun IconTap(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
    }
}
