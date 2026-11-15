package com.gezgin.zikirmatik

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import java.time.LocalDate

enum class Scr { SAYAC, ZIKIRLER, KUTUPHANE, TESBIHAT, ISTATISTIK, AYARLAR, ODAK }

class MainActivity : ComponentActivity() {
    var onVolumeKey: (() -> Boolean)? = null
    private var barsDark: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyBars(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { ZikirApp(this) }
    }

    fun applyBars(dark: Boolean) {
        if (barsDark == dark) return
        barsDark = dark
        val t = android.graphics.Color.TRANSPARENT
        val style = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val h = onVolumeKey
            if (h != null) {
                if ((event?.repeatCount ?: 0) > 0) return true
                if (h()) return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun ZikirApp(act: MainActivity) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val st = remember { AppState(ctx.applicationContext, Repo(ctx), Feedback(ctx.applicationContext), scope).also { it.load() } }
    DisposableEffect(Unit) { onDispose { st.fb.release() } }

    var screen by rememberSaveable { mutableStateOf(Scr.SAYAC) }
    var sheet by remember { mutableStateOf<SheetKind?>(null) }
    var intro by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1300); intro = false }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { st.reloadCounts() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { ZikirWidget.refresh(ctx) }

    val pal = PALETLER[st.settings.theme.coerceIn(0, PALETLER.lastIndex)]
    LaunchedEffect(pal.dark) { act.applyBars(pal.dark) }

    // Ses tuşları: sadece sayaç ve odak ekranında, panel kapalıyken
    val volumeHandler by rememberUpdatedState {
        if (st.settings.volumeKeys && sheet == null && (screen == Scr.SAYAC || screen == Scr.ODAK)) { st.tap(); true } else false
    }
    DisposableEffect(Unit) {
        act.onVolumeKey = { volumeHandler() }
        onDispose { act.onVolumeKey = null }
    }

    // Yedekleme / geri yükleme / bildirim izni
    val exportL = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(st.repo.exportJson().toByteArray()) } }.isSuccess
            st.toast(if (ok) "Yedek kaydedildi" else "Yedek kaydedilemedi")
        }
    }
    val importL = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull()
            if (text != null && st.repo.importJson(text)) {
                st.load()
                Reminders.schedule(ctx, st.settings)
                st.toast("Yedek geri yüklendi")
            } else st.toast("Bu dosya geçerli bir Zikirmatik yedeği değil")
        }
    }
    val notifyL = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) st.toast("Bildirim izni verilmedi; hatırlatıcılar görünmeyecek")
    }
    val askNotify = {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifyL.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val go: (Scr) -> Unit = { s -> sheet = null; screen = s }
    BackHandler(enabled = screen != Scr.SAYAC) { screen = if (screen == Scr.KUTUPHANE) Scr.ZIKIRLER else Scr.SAYAC }

    val baseDensity = LocalDensity.current
    MaterialTheme(
        colorScheme = if (pal.dark) darkColorScheme(primary = pal.gold, surface = pal.sheet, background = pal.bgMid)
        else lightColorScheme(primary = pal.goldDeep, surface = pal.sheet, background = pal.bgMid),
    ) {
        CompositionLocalProvider(
            LocalPal provides pal,
            LocalDensity provides Density(baseDensity.density, baseDensity.fontScale * st.settings.fontScale),
            LocalTextStyle provides TextStyle(fontFamily = Sans, color = pal.text),
        ) {
            Box(Modifier.fillMaxSize()) {
                if (screen != Scr.ODAK) Sky(pal)
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        (fadeIn(tween(320)) + scaleIn(tween(320), initialScale = 0.985f)) togetherWith fadeOut(tween(180))
                    },
                    label = "ekran",
                ) { s ->
                    when (s) {
                        Scr.SAYAC -> CounterScreen(st, go) { sheet = it }
                        Scr.ZIKIRLER -> ZikirlerScreen(st, { screen = Scr.SAYAC }, go) { sheet = it }
                        Scr.KUTUPHANE -> LibraryScreen(st) { screen = Scr.ZIKIRLER }
                        Scr.TESBIHAT -> TesbihatScreen(st, { screen = Scr.SAYAC }) { sheet = it }
                        Scr.ISTATISTIK -> StatsScreen(st) { screen = Scr.SAYAC }
                        Scr.AYARLAR -> SettingsScreen(
                            st, { screen = Scr.SAYAC },
                            onExport = { exportL.launch("zikirmatik-yedek-${LocalDate.now()}.json") },
                            onImport = { importL.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                            askNotify = askNotify,
                        )
                        Scr.ODAK -> FocusScreen(st) { screen = Scr.SAYAC }
                    }
                }
                SnackbarHost(st.snack, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) { LuxSnack(it) }
                AnimatedVisibility(intro, enter = fadeIn(), exit = fadeOut(tween(700))) {
                    // Açılış ekranındaki dokunuşlar alttaki sayaca geçmesin; dokununca kapanır
                    Box(
                        Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { intro = false },
                    ) { Intro() }
                }
            }

            when (val k = sheet) {
                null -> Unit
                SheetKind.Menu -> MenuSheet({ sheet = null }, go) { sheet = SheetKind.Edit(null) }
                SheetKind.Target -> TargetSheet(st.current.target, { st.setTarget(it); sheet = null }) { sheet = null }
                is SheetKind.Edit -> EditZikirSheet(k.zikir, { o, a, an, f, t -> st.saveZikir(k.zikir, o, a, an, f, t); sheet = null }) { sheet = null }
                is SheetKind.VirdEdit -> VirdEditSheet(st, k.vird) { sheet = null }
            }
        }
    }
}
