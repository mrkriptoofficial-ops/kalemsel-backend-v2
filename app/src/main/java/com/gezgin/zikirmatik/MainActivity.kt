package com.gezgin.zikirmatik

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
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

/** Uygulama durumu ekran yeniden oluşturulsa da (tema, yazı boyutu, DeX) korunur. */
class ZikirVM(app: Application) : AndroidViewModel(app) {
    val st = AppState(app, Repo(app), Feedback(app), viewModelScope).also { it.load() }
    override fun onCleared() = st.fb.release()
}

class MainActivity : ComponentActivity() {
    var onVolumeKey: (() -> Unit)? = null
    var volumeActive: () -> Boolean = { false }
    var onFocusBack: (() -> Unit)? = null
    private var barsDark: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyBars(true)
        // Zorla durdurma, "derin uyku" ya da yedekten geri yükleme sonrası alarmlar kaybolmasın
        Reminders.schedule(applicationContext, Repo(this).loadSettings())
        setContent { ZikirApp(this) }
    }

    fun applyBars(dark: Boolean) {
        if (barsDark == dark) return
        barsDark = dark
        val t = android.graphics.Color.TRANSPARENT
        val style = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    private fun isVolume(k: Int) = k == KeyEvent.KEYCODE_VOLUME_DOWN || k == KeyEvent.KEYCODE_VOLUME_UP

    // Ses tuşları yalnızca sayaç/odak ekranında ve ayar açıkken sayar; aksi halde ses normal ayarlanır
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isVolume(keyCode) && volumeActive()) {
            if ((event?.repeatCount ?: 0) == 0) onVolumeKey?.invoke()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean =
        if (isVolume(keyCode) && volumeActive()) true else super.onKeyUp(keyCode, event)

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) onFocusBack?.invoke()
    }
}

@Composable
fun ZikirApp(act: MainActivity) {
    val ctx = LocalContext.current
    val st = viewModel<ZikirVM>().st

    var screen by rememberSaveable { mutableStateOf(Scr.SAYAC) }
    var sheet by remember { mutableStateOf<SheetKind?>(null) }
    var intro by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1300); intro = false }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { st.reloadCounts() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { ZikirWidget.refresh(ctx) }

    val pal = PALETLER[st.settings.theme.coerceIn(0, PALETLER.lastIndex)]
    val lang = st.settings.lang
    I18n.apply(lang)
    LaunchedEffect(pal.dark) { act.applyBars(pal.dark) }

    // Ses tuşları: sadece sayaç ve odak ekranında, panel kapalıyken
    val active by rememberUpdatedState(st.settings.volumeKeys && sheet == null && (screen == Scr.SAYAC || screen == Scr.ODAK))
    DisposableEffect(Unit) {
        act.onVolumeKey = { st.tap() }
        act.volumeActive = { active }
        // Samsung açılır pencere gibi durumlarda widget'tan gelen sayımları kaçırma
        act.onFocusBack = { st.reloadCounts() }
        onDispose { act.onVolumeKey = null; act.volumeActive = { false }; act.onFocusBack = null }
    }

    // Ekran yalnızca sayaç ve odak modunda açık kalır (pil ve OLED yanması için)
    val keepOn = screen == Scr.SAYAC || screen == Scr.ODAK
    DisposableEffect(keepOn) {
        if (keepOn) act.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else act.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { act.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // Yedekleme / geri yükleme / bildirim izni
    val exportL = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(st.repo.exportJson().toByteArray()) } }.isSuccess
            st.toast(if (ok) L("Yedek kaydedildi", "Backup saved") else L("Yedek kaydedilemedi", "Backup could not be saved"))
        }
    }
    val importL = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull()
            if (text != null && st.repo.importJson(text)) {
                st.load()
                Reminders.schedule(ctx, st.settings)
                st.toast(L("Yedek geri yüklendi", "Backup restored"))
            } else st.toast(L("Bu dosya geçerli bir Zikirmatik yedeği değil", "This file is not a valid Zikirmatik backup"))
        }
    }
    // Bildirim izni reddedilirse hatırlatıcıları kapat ve kullanıcıyı ayara yönlendir
    val openNotifySettings = {
        runCatching {
            // Uygulamaya özel bildirim ayarı ekranı Android 8.0 ile geldi; öncesinde uygulama bilgisi açılır
            val i = if (Build.VERSION.SDK_INT >= 26) {
                Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, ctx.packageName)
            } else {
                Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + ctx.packageName))
            }
            ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Unit
    }
    val notifyL = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            st.update { it.copy(remMorning = false, remEvening = false) }
            st.toast(L("Bildirim izni verilmedi; hatırlatıcılar kapatıldı", "Notification permission denied; reminders turned off"))
            openNotifySettings()
        }
    }
    val askNotify = {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifyL.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) {
            st.toast(L("Bildirimler kapalı; hatırlatıcı için açmalısın", "Notifications are off; turn them on for reminders"))
            openNotifySettings()
        }
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
            // Dil değişince tüm arayüz yeni dilde baştan çizilir
            key(lang) {
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
}
