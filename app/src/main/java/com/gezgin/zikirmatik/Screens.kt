package com.gezgin.zikirmatik

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

private val TRL = Locale.forLanguageTag("tr")

// ───────────── Zikirlerim ─────────────
@Composable
fun ZikirlerScreen(st: AppState, back: () -> Unit, go: (Scr) -> Unit, sheet: (SheetKind) -> Unit) {
    val p = LocalPal.current
    var dragId by remember { mutableStateOf<Long?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var rowH by remember { mutableFloatStateOf(0f) }
    val spacingPx = with(LocalDensity.current) { 10.dp.toPx() }
    ScreenFrame(L("Zikirlerim", "My Dhikr"), back, action = { GlyphButton(G.PLUS, L("Yeni zikir", "New dhikr")) { sheet(SheetKind.Edit(null)) } }) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton(L("Kütüphane", "Library"), Modifier.weight(1f), G.BOOK) { go(Scr.KUTUPHANE) }
                    GhostButton(L("Tesbihat", "Sequences"), Modifier.weight(1f), G.BEADS) { go(Scr.TESBIHAT) }
                }
                Spacer(Modifier.height(4.dp))
                Text(L("Sıralamak için bir zikre uzun basıp sürükle.", "Long-press and drag a dhikr to reorder."), style = bodyStyle(p.sub).copy(fontSize = 12.sp, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
            }
            itemsIndexed(st.zikirs, key = { _, z -> z.id }) { i, z ->
                val on = z.id == st.selId && st.session == null
                val shape = RoundedCornerShape(22.dp)
                val dragging = dragId == z.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .then(if (dragging) Modifier.zIndex(1f) else Modifier.animateItem())
                        .onSizeChanged { rowH = it.height.toFloat() + spacingPx }
                        .graphicsLayer {
                            if (dragging) { translationY = dragY; scaleX = 1.03f; scaleY = 1.03f; shadowElevation = 18f }
                        }
                        // Uzun bas → sürükle: sıralamayı değiştir
                        .pointerInput(z.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { dragId = z.id; dragY = 0f; st.fb.tap(if (st.settings.haptic == 0) 0 else 2) },
                                onDragEnd = { dragId = null; dragY = 0f },
                                onDragCancel = { dragId = null; dragY = 0f },
                            ) { change, amount ->
                                change.consume()
                                dragY += amount.y
                                val cur = st.zikirs.firstOrNull { it.id == z.id } ?: return@detectDragGesturesAfterLongPress
                                if (rowH > 0f && dragY > rowH * 0.6f) { st.move(cur, 1); dragY -= rowH }
                                else if (rowH > 0f && dragY < -rowH * 0.6f) { st.move(cur, -1); dragY += rowH }
                            }
                        }
                        .semantics {
                            customActions = listOf(
                                CustomAccessibilityAction(L("Yukarı taşı", "Move up")) { st.move(z, -1); true },
                                CustomAccessibilityAction(L("Aşağı taşı", "Move down")) { st.move(z, 1); true },
                            )
                        }
                        .then(if (on) Modifier.clip(shape).background(p.gold.copy(alpha = 0.12f)).border(1.dp, p.gold.copy(alpha = 0.7f), shape) else Modifier.glass(p, shape))
                        .clickable { st.endSession(); st.select(z.id); back() }
                        .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${i + 1}",
                        style = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = p.goldText, fontFeatureSettings = NUM_FEATURES),
                        modifier = Modifier.widthIn(min = 28.dp),
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Text(z.okunus, style = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.5.sp, color = p.text), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (z.arapca.isNotBlank()) {
                            Text(
                                z.arapca,
                                style = TextStyle(fontFamily = Arabic, fontSize = 17.sp, lineHeight = 30.sp, color = p.sub, textDirection = TextDirection.Rtl),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            L("${fmt(z.count)}  ·  hedef ${fmt(z.target)}", "${fmt(z.count)}  ·  target ${fmt(z.target)}"),
                            style = labelStyle(p.sub).copy(fontSize = 10.sp, letterSpacing = 1.sp, fontFeatureSettings = NUM_FEATURES),
                        )
                    }
                    Glyph(G.GRIP, p.faint, 20.dp, Modifier.padding(horizontal = 4.dp))
                    GlyphButton(G.EDIT, L("Düzenle", "Edit"), 40.dp, bordered = false) { sheet(SheetKind.Edit(z)) }
                    if (st.zikirs.size > 1) GlyphButton(G.TRASH, L("Sil", "Delete"), 40.dp, bordered = false) { st.delete(z) }
                }
            }
        }
    }
}

// ───────────── Kütüphane ─────────────
@Composable
fun LibraryScreen(st: AppState, back: () -> Unit) {
    val p = LocalPal.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(L("Sünnet", "Sunnah"), L("Günlük", "Daily"), L("Salavât", "Salawat"), L("Esmâ", "Names"))
    ScreenFrame(L("Kütüphane", "Library"), back) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp).glass(p, RoundedCornerShape(50)).padding(4.dp),
        ) {
            tabs.forEachIndexed { i, t ->
                val on = tab == i
                Box(
                    Modifier.weight(1f).heightIn(min = 40.dp).clip(RoundedCornerShape(50))
                        .then(if (on) Modifier.background(p.goldBrush) else Modifier)
                        .clickable(role = Role.Tab) { tab = i },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(t, style = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (on) p.onGold else p.text), maxLines = 1)
                }
            }
        }
        AnimatedContent(tab, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "sekme") { t ->
            val items = when (t) { 0 -> LIB_SUNNET; 1 -> LIB_GUNLUK; 2 -> LIB_SALAVAT; else -> LIB_ESMA }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (t == 0) item {
                    Text(
                        L("Peygamber Efendimiz'in (s.a.v.) tavsiye ettiği zikirler. Her birinin faziletini bildiren hadisin kaynağı yanında yazılıdır.", "Dhikr recommended by the Prophet (peace be upon him). The hadith source for each virtue is noted beside it."),
                        style = bodyStyle(p.sub).copy(textAlign = TextAlign.Center, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                items(items, key = { it.okunus }) { item ->
                    val have = st.hasLib(item)
                    Row(
                        Modifier.fillMaxWidth().glass(p, RoundedCornerShape(22.dp)).padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.arapca,
                                style = TextStyle(fontFamily = Arabic, fontSize = 22.sp, lineHeight = 38.sp, color = p.text, textDirection = TextDirection.Rtl, textAlign = TextAlign.End),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(item.okunus, style = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, color = p.goldText))
                            Spacer(Modifier.height(2.dp))
                            Text(item.anlam, style = bodyStyle(p.sub).copy(fontSize = 13.sp))
                            if (item.fazilet.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(item.fazilet, style = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 19.sp, color = p.sub))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        if (have) GlyphButton(G.CHECK, L("Eklendi", "Added"), 40.dp, bordered = false, tint = p.sub) {}
                        else GlyphButton(G.PLUS, L("Zikirlerime ekle", "Add to my dhikr"), 40.dp) { st.addFromLib(item) }
                    }
                }
            }
        }
    }
}

// ───────────── Tesbihat ─────────────
@Composable
fun TesbihatScreen(st: AppState, back: () -> Unit, sheet: (SheetKind) -> Unit) {
    val p = LocalPal.current
    ScreenFrame(L("Tesbihat", "Sequences"), back, action = { GlyphButton(G.PLUS, L("Yeni tesbihat", "New sequence")) { sheet(SheetKind.VirdEdit(null)) } }) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    L("Sıralı zikir setleri. Bir adımın hedefi dolunca sonraki adıma kendiliğinden geçilir.", "Dhikr sequences. When a step reaches its target, the next step starts automatically."),
                    style = bodyStyle(p.sub).copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                )
            }
            items(st.virds, key = { it.id }) { v ->
                val active = st.session?.virdId == v.id
                Column(
                    Modifier.fillMaxWidth()
                        .then(if (active) Modifier.clip(RoundedCornerShape(24.dp)).border(1.dp, p.gold, RoundedCornerShape(24.dp)) else Modifier)
                        .glass(p, RoundedCornerShape(24.dp)).padding(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(v.name, style = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = p.text))
                            Text(L("${v.steps.size} adım · ${fmt(v.total)} zikir", "${v.steps.size} steps · ${fmt(v.total)} dhikr"), style = labelStyle(p.sub).copy(letterSpacing = 1.sp, fontFeatureSettings = NUM_FEATURES))
                        }
                        GlyphButton(G.EDIT, L("Düzenle", "Edit"), 40.dp, bordered = false) { sheet(SheetKind.VirdEdit(v)) }
                        GlyphButton(G.TRASH, L("Sil", "Delete"), 40.dp, bordered = false) { st.deleteVird(v) }
                    }
                    Spacer(Modifier.height(10.dp))
                    v.steps.forEachIndexed { i, s ->
                        Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(p.gold))
                            Spacer(Modifier.width(10.dp))
                            Text(s.okunus, style = bodyStyle(p.text), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(fmt(s.target), style = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = p.goldText, fontFeatureSettings = NUM_FEATURES))
                        }
                        if (i == v.steps.lastIndex) Spacer(Modifier.height(8.dp))
                    }
                    PrimaryButton(if (active) L("Devam et", "Continue") else L("Başla", "Start"), Modifier.fillMaxWidth(), enabled = v.steps.isNotEmpty(), glyph = G.PLAY) {
                        if (!active) st.startVird(v)
                        back()
                    }
                }
            }
        }
    }
}

// ───────────── İstatistik ─────────────
@Composable
fun StatsScreen(st: AppState, back: () -> Unit) {
    val p = LocalPal.current
    ScreenFrame(L("İstatistik", "Statistics"), back) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Tile(L("Bugün", "Today"), fmt(st.today()), Modifier.weight(1f))
                    Tile(L("Bu hafta", "This week"), fmt(st.weekSum()), Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Tile(L("Bu ay", "This month"), fmt(st.monthSum()), Modifier.weight(1f))
                    Tile(L("Toplam", "Total"), fmt(st.total), Modifier.weight(1f))
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().glass(p, RoundedCornerShape(24.dp)).padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StarLogo(54.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(L("GÜNLÜK SERİ", "DAILY STREAK"), style = labelStyle(p.sub))
                        Text(
                            L("${st.streak()} gün", "${st.streak()} days"),
                            style = TextStyle(brush = p.numberBrush, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, fontFeatureSettings = NUM_FEATURES),
                        )
                        Text(L("En uzun seri: ${st.bestStreak} gün", "Longest streak: ${st.bestStreak} days"), style = bodyStyle(p.sub).copy(fontSize = 13.sp))
                    }
                }
            }
            item { WeekBars(st) }
            item { Heatmap(st) }
            item {
                val today = st.lapsToday()
                Column(Modifier.fillMaxWidth().glass(p, RoundedCornerShape(24.dp)).padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(L("TAMAMLANAN TURLAR", "COMPLETED ROUNDS"), style = labelStyle(p.sub), modifier = Modifier.weight(1f))
                        Text(L("Toplam ${fmt(st.lapsTotal())}", "Total ${fmt(st.lapsTotal())}"), style = labelStyle(p.goldText).copy(letterSpacing = 1.sp, fontFeatureSettings = NUM_FEATURES))
                    }
                    Spacer(Modifier.height(10.dp))
                    if (today.isEmpty()) {
                        Text(L("Bugün henüz tamamlanan tur yok.", "No rounds completed today yet."), style = bodyStyle(p.sub).copy(fontSize = 13.sp))
                    } else {
                        today.entries.sortedByDescending { it.value }.forEach { (name, n) ->
                            Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(p.gold))
                                Spacer(Modifier.width(10.dp))
                                Text(name, style = bodyStyle(p.text), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("× ${fmt(n)}", style = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = p.goldText, fontFeatureSettings = NUM_FEATURES))
                            }
                        }
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().glass(p, RoundedCornerShape(24.dp)).padding(20.dp)) {
                    Text(L("GÜNLÜK HEDEF", "DAILY GOAL"), style = labelStyle(p.sub))
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (st.settings.dailyGoal == 0) L("Kapalı", "Off") else "${fmt(st.today())} / ${fmt(st.settings.dailyGoal)}",
                            style = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = p.text, fontFeatureSettings = NUM_FEATURES),
                            modifier = Modifier.weight(1f),
                        )
                        Stepper(
                            if (st.settings.dailyGoal == 0) "—" else fmt(st.settings.dailyGoal),
                            onMinus = { st.update { s -> s.copy(dailyGoal = (s.dailyGoal - 100).coerceAtLeast(0)) } },
                            onPlus = { st.update { s -> s.copy(dailyGoal = (s.dailyGoal + 100).coerceAtMost(100_000)) } },
                        )
                    }
                }
            }
            item {
                val top = st.zikirs.sortedByDescending { it.count }.filter { it.count > 0 }.take(6)
                if (top.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth().glass(p, RoundedCornerShape(24.dp)).padding(20.dp)) {
                        Text(L("EN ÇOK ÇEKİLENLER", "MOST RECITED"), style = labelStyle(p.sub))
                        Spacer(Modifier.height(10.dp))
                        val max = top.first().count.toFloat()
                        top.forEach { z ->
                            Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(z.okunus, style = bodyStyle(p.text), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(fmt(z.count), style = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = p.goldText, fontFeatureSettings = NUM_FEATURES))
                            }
                            Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(50)).background(p.faint.copy(alpha = 0.25f))) {
                                Box(Modifier.fillMaxWidth(z.count / max).fillMaxHeight().clip(RoundedCornerShape(50)).background(p.goldBrush))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tile(label: String, value: String, modifier: Modifier) {
    val p = LocalPal.current
    Column(modifier.glass(p, RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 16.dp)) {
        Text(label.upperL(), style = labelStyle(p.sub))
        Spacer(Modifier.height(4.dp))
        Text(value, style = TextStyle(brush = p.numberBrush, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, fontFeatureSettings = NUM_FEATURES), maxLines = 1)
    }
}

@Composable
private fun WeekBars(st: AppState) {
    val p = LocalPal.current
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val vals = days.map { st.history[dayKey(it)] ?: 0 }
    val max = (vals.maxOrNull() ?: 0).coerceAtLeast(1)
    Column(Modifier.fillMaxWidth().glass(p, RoundedCornerShape(24.dp)).padding(20.dp)) {
        Text(L("SON 7 GÜN", "LAST 7 DAYS"), style = labelStyle(p.sub))
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            days.forEachIndexed { i, d ->
                val v = vals[i]
                val isToday = i == 6
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (v > 0) compact(v) else "",
                        style = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = if (isToday) p.goldText else p.sub, fontFeatureSettings = NUM_FEATURES),
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(4.dp))
                    val h = (v / max.toFloat()).coerceIn(0f, 1f)
                    Box(
                        Modifier.width(18.dp).height((96 * h).coerceAtLeast(3f).dp).clip(RoundedCornerShape(50))
                            .background(if (v == 0) Brush.verticalGradient(listOf(p.faint, p.faint)) else if (isToday) p.goldBrush else Brush.verticalGradient(listOf(p.goldLight.copy(alpha = 0.75f), p.goldDeep.copy(alpha = 0.55f))))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        d.dayOfWeek.getDisplayName(JTextStyle.SHORT, I18n.locale).take(3),
                        style = TextStyle(fontFamily = Sans, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp, color = if (isToday) p.text else p.sub),
                    )
                }
            }
        }
    }
}

private fun compact(v: Int): String = when {
    v >= 10_000 -> "${v / 1000}" + L("B", "K")
    v >= 1_000 -> String.format(I18n.locale, "%.1f", v / 1000f) + L("B", "K")
    else -> "$v"
}

@Composable
private fun Heatmap(st: AppState) {
    val p = LocalPal.current
    val today = LocalDate.now()
    val start = today.minusWeeks(5).with(DayOfWeek.MONDAY)
    val days = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.toList()
    val max = days.maxOfOrNull { st.history[dayKey(it)] ?: 0 }?.coerceAtLeast(1) ?: 1
    Column(Modifier.fillMaxWidth().glass(p, RoundedCornerShape(24.dp)).padding(20.dp)) {
        Text(L("TAKVİM", "CALENDAR"), style = labelStyle(p.sub))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(L("Pt", "Mo"), L("Sa", "Tu"), L("Ça", "We"), L("Pe", "Th"), L("Cu", "Fr"), L("Ct", L("Sa", "Tu")), L("Pz", "Su")).forEach {
                Text(it, style = labelStyle(p.sub).copy(letterSpacing = 0.sp), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(6.dp))
        val weeks = days.chunked(7)
        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                (0 until 7).forEach { i ->
                    val d = week.getOrNull(i)
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp)) {
                        if (d != null) {
                            val v = st.history[dayKey(d)] ?: 0
                            val f = if (v == 0) 0f else 0.25f + 0.75f * (v / max.toFloat())
                            val isToday = d == today
                            Canvas(Modifier.fillMaxSize()) {
                                val cr = CornerRadius(size.minDimension * 0.28f)
                                drawRoundRect(if (v == 0) p.hair else lerp(p.goldDeep.copy(alpha = 0.35f), p.goldLight, f), cornerRadius = cr)
                                if (isToday) drawRoundRect(p.gold, cornerRadius = cr, style = Stroke(2.dp.toPx()))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ───────────── Ayarlar ─────────────
@Composable
fun SettingsScreen(st: AppState, back: () -> Unit, onExport: () -> Unit, onImport: () -> Unit, askNotify: () -> Unit) {
    val p = LocalPal.current
    val s = st.settings
    ScreenFrame(L("Ayarlar", "Settings"), back) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 48.dp),
        ) {
            item {
                SectionTitle(L("Görünüm", "Appearance"))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PALETLER.forEachIndexed { i, pal -> ThemeSwatch(pal, s.theme == i, Modifier.weight(1f)) { st.update { it.copy(theme = i) } } }
                }
                Spacer(Modifier.height(14.dp))
                Text(L("Tespih", "Prayer beads"), style = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = p.text))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BEADS.forEachIndexed { i, b -> BeadSwatch(b, s.bead == i, Modifier.weight(1f)) { st.update { it.copy(bead = i) } } }
                }
                Spacer(Modifier.height(14.dp))
                Text(L("Dil", "Language"), style = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = p.text))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(L("Sistem", "System") to 0, "Türkçe" to 1, "English" to 2).forEach { (t, v) ->
                        GoldChip(t, s.lang == v) { st.update { it.copy(lang = v) } }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(L("Yazı boyutu", "Text size"), style = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = p.text))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(L("Normal", "Normal") to 1f, L("Büyük", "Large") to 1.15f, L("Çok büyük", "Extra large") to 1.3f).forEach { (t, v) ->
                        GoldChip(t, abs(s.fontScale - v) < 0.01f) { st.update { it.copy(fontScale = v) } }
                    }
                }
            }
            item {
                SectionTitle(L("Sayma", "Counting"))
                SettingRow(L("Hedefte dur", "Stop at target"), L("Hedefe ulaşınca sayaç durur. Kapalıyken turları sayarak devam eder.", "Counter stops at the target. When off, it keeps counting rounds.")) {
                    LuxSwitch(s.stopAtTarget) { v -> st.update { it.copy(stopAtTarget = v) } }
                }
                SettingRow(L("Ses tuşlarıyla say", "Count with volume keys"), L("Ekrana bakmadan, ses açma/kısma tuşlarıyla zikir çek.", "Count with the volume buttons without looking at the screen.")) {
                    LuxSwitch(s.volumeKeys) { v -> st.update { it.copy(volumeKeys = v) } }
                }
                Text(L("Çift dokunma koruması", "Double-tap guard"), style = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = p.text), modifier = Modifier.padding(top = 10.dp))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(L("Kapalı", "Off") to 0, L("Normal", "Normal") to 70, L("Güçlü", "Strong") to 160).forEach { (t, v) -> GoldChip(t, s.guardMs == v) { st.update { it.copy(guardMs = v) } } }
                }
            }
            item {
                SectionTitle(L("Titreşim ve ses", "Haptics and sound"))
                Text(L("Titreşim gücü", "Vibration strength"), style = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = p.text))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(L("Kapalı", "Off"), L("Hafif", "Light"), L("Normal", "Normal"), L("Güçlü", "Strong")).forEachIndexed { i, t ->
                        GoldChip(t, s.haptic == i) { st.update { it.copy(haptic = i) }; st.fb.tap(i) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(L("Ara titreşim", "Interval vibration"), style = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = p.text))
                Text(L("Hedefe ulaşmadan, belirli aralıklarla çift titreşim verir.", "Double vibration at set intervals before the target."), style = bodyStyle(p.sub).copy(fontSize = 12.5.sp))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(L("Kapalı", "Off") to 0, L("Her 10", "Every 10") to 10, L("Her 33", "Every 33") to 33).forEach { (t, v) -> GoldChip(t, s.milestone == v) { st.update { it.copy(milestone = v) } } }
                }
                SettingRow(L("Tespih sesi", "Bead sound"), L("Her dokunuşta yumuşak bir tane sesi.", "A soft bead click on every tap.")) {
                    LuxSwitch(s.sound) { v -> st.update { it.copy(sound = v) }; if (v) st.fb.click(true) }
                }
            }
            item {
                SectionTitle(L("Hatırlatıcılar", "Reminders"))
                ReminderRow(L("Sabah zikri", "Morning dhikr"), s.remMorning, s.morningMin,
                    onToggle = { v -> if (v) askNotify(); st.update { it.copy(remMorning = v) } },
                    onTime = { m -> st.update { it.copy(morningMin = m) } })
                ReminderRow(L("Akşam zikri", "Evening dhikr"), s.remEvening, s.eveningMin,
                    onToggle = { v -> if (v) askNotify(); st.update { it.copy(remEvening = v) } },
                    onTime = { m -> st.update { it.copy(eveningMin = m) } })
                Text(
                    L("Hatırlatıcılar sabit saatte çalışır. Namaz vaktine göre hatırlatma, konum verisi gerektirdiği için bu sürümde yok.", "Reminders run at a fixed time. Prayer-time reminders need location data and are not in this version."),
                    style = bodyStyle(p.sub).copy(fontSize = 12.sp), modifier = Modifier.padding(top = 4.dp),
                )
            }
            item {
                SectionTitle(L("Veriler", "Data"))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton(L("Yedekle", "Back up"), Modifier.weight(1f), G.EXPORT, onExport)
                    GhostButton(L("Geri yükle", "Restore"), Modifier.weight(1f), G.IMPORT, onImport)
                }
                Text(
                    L("Yedek dosyası zikirlerini, sayaçlarını, tesbihatlarını, geçmişini ve ayarlarını birlikte saklar.", "The backup file stores your dhikr, counts, sequences, history and settings together."),
                    style = bodyStyle(p.sub).copy(fontSize = 12.sp), modifier = Modifier.padding(top = 8.dp),
                )
            }
            item {
                SectionTitle(L("Hakkında", "About"))
                Text(
                    L("Zikirmatik 2.6\nYazı tipleri: Amiri, Cormorant Garamond, Manrope (SIL Open Font License).", "Zikirmatik 2.6\nFonts: Amiri, Cormorant Garamond, Manrope (SIL Open Font License)."),
                    style = bodyStyle(p.sub).copy(fontSize = 12.5.sp),
                )
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Ornament(160.dp, 0.7f) }
            }
        }
    }
}

private fun abs(f: Float) = kotlin.math.abs(f)

@Composable
private fun BeadSwatch(m: BeadMat, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPal.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.clip(shape)
            .then(if (selected) Modifier.border(2.dp, p.gold, shape) else Modifier.glass(p, shape))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = nameL(m.name) }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(30.dp)) {
            val r = size.minDimension / 2
            drawCircle(Brush.radialGradient(0f to m.light, 0.55f to m.base, 1f to m.edge, center = Offset(r * 0.7f, r * 0.64f), radius = r * 1.45f))
            drawCircle(Color.White.copy(alpha = 0.9f * m.spec), r * 0.09f, Offset(r * 0.7f, r * 0.62f))
        }
        Spacer(Modifier.height(6.dp))
        Text(nameL(m.name), style = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = p.text), maxLines = 1)
    }
}

@Composable
private fun ThemeSwatch(pal: Pal, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPal.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(pal.bgTop, pal.bgBottom)))
            .border(if (selected) 2.dp else 1.dp, if (selected) p.gold else p.hair, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = L("${pal.name} teması", "${nameL(pal.name)} theme") }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(42.dp)) {
            drawCircle(Brush.radialGradient(listOf(pal.dome0, pal.dome1, pal.dome2), center = Offset(size.width * 0.35f, size.height * 0.3f), radius = size.width * 0.8f))
            drawCircle(pal.gold, style = Stroke(1.5.dp.toPx()))
            drawArc(pal.goldLight, -90f, 230f, false, Offset(6.dp.toPx(), 6.dp.toPx()), Size(size.width - 12.dp.toPx(), size.height - 12.dp.toPx()), style = Stroke(2.dp.toPx()))
        }
        Spacer(Modifier.height(8.dp))
        Text(nameL(pal.name), style = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = pal.text))
    }
}

@Composable
private fun ReminderRow(title: String, on: Boolean, minutes: Int, onToggle: (Boolean) -> Unit, onTime: (Int) -> Unit) {
    val p = LocalPal.current
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).glass(p, RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 6.dp)) {
        SettingRow(title, if (on) L("Her gün %02d:%02d", "Daily at %02d:%02d").format(minutes / 60, minutes % 60) else L("Kapalı", "Off")) {
            LuxSwitch(on, onToggle)
        }
        if (on) {
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(L("SAAT", "HOUR"), style = labelStyle(p.sub))
                Stepper("%02d".format(minutes / 60), { onTime(((minutes / 60 + 23) % 24) * 60 + minutes % 60) }, { onTime(((minutes / 60 + 1) % 24) * 60 + minutes % 60) })
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(L("DAKİKA", "MINUTE"), style = labelStyle(p.sub))
                Stepper("%02d".format(minutes % 60), { onTime((minutes / 60) * 60 + (minutes % 60 + 55) % 60) }, { onTime((minutes / 60) * 60 + (minutes % 60 + 5) % 60) })
            }
        }
    }
}

// ───────────── Odak modu ─────────────
@Composable
fun FocusScreen(st: AppState, exit: () -> Unit) {
    val p = LocalPal.current
    val ctx = LocalContext.current
    DisposableEffect(Unit) {
        val w = (ctx as? Activity)?.window
        val old = w?.attributes?.screenBrightness ?: -1f
        w?.attributes = w?.attributes?.also { it.screenBrightness = 0.12f }
        onDispose { w?.attributes = w?.attributes?.also { it.screenBrightness = old } }
    }
    val d = st.display()
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .semantics { contentDescription = L("Odak modu. ${d.okunus}. ${d.shown}, hedef ${d.target}. Saymak için dokunun.", "Focus mode. ${d.okunus}. ${d.shown} of ${d.target}. Tap to count.") }
            .clickable(interactionSource = interaction, indication = null) { st.tap() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val big = with(LocalDensity.current) { (if (d.shown >= 1000) 92.dp else 120.dp).toSp() }
            Text(d.okunus.uppercase(TRL), style = labelStyle(GECE.sub.copy(alpha = 0.85f)).copy(fontSize = 12.sp, letterSpacing = 3.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                "${d.shown}",
                style = TextStyle(brush = GECE.numberBrush, fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = big, fontFeatureSettings = NUM_FEATURES),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Spacer(Modifier.height(8.dp))
            Box(Modifier.width(180.dp).height(2.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.08f))) {
                Box(Modifier.fillMaxWidth(d.progress).fillMaxHeight().background(GECE.goldBrush))
            }
            Spacer(Modifier.height(10.dp))
            Text(L("HEDEF ${fmt(d.target)}", "TARGET ${fmt(d.target)}"), style = labelStyle(GECE.sub.copy(alpha = 0.75f)).copy(fontFeatureSettings = NUM_FEATURES))
        }
        Box(Modifier.align(Alignment.TopEnd).systemBarsPadding().padding(16.dp)) {
            CompositionLocalProvider(LocalPal provides GECE) {
                GlyphButton(G.CLOSE, L("Odak modundan çık", "Exit focus mode"), 48.dp, bordered = false, tint = GECE.sub.copy(alpha = 0.75f), onClick = exit)
            }
        }
        Box(Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(16.dp)) {
            CompositionLocalProvider(LocalPal provides GECE) {
                GlyphButton(G.UNDO, L("Bir eksilt", "Undo one"), 48.dp, bordered = false, tint = GECE.sub.copy(alpha = 0.75f)) { st.undo() }
            }
        }
        Text(
            L("Ekranın her yeri sayar", "Tap anywhere to count"),
            style = labelStyle(GECE.sub.copy(alpha = 0.6f)),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 30.dp),
        )
    }
}

// ───────────── Açılış ─────────────
@Composable
fun Intro() {
    val p = LocalPal.current
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(p.bgTop, p.bgMid, p.bgBottom))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            StarLogo(96.dp)
            Spacer(Modifier.height(26.dp))
            Text(L("ZİKİRMATİK", "ZIKIRMATIK"), style = TextStyle(brush = p.goldBrush, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = 10.sp), modifier = Modifier.padding(start = 10.dp))
            Spacer(Modifier.height(10.dp))
            Ornament(120.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                L("Kalpler ancak Allah'ı anmakla huzur bulur", "Hearts find peace only in the remembrance of Allah"),
                style = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 17.sp, color = p.sub, textAlign = TextAlign.Center),
            )
            Text(L("Ra'd, 13/28", "Ar-Ra'd 13:28"), style = labelStyle(p.sub).copy(letterSpacing = 1.5.sp), modifier = Modifier.padding(top = 6.dp))
        }
    }
}
