package com.gezgin.zikirmatik

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed interface SheetKind {
    data object Menu : SheetKind
    data object Target : SheetKind
    data class Edit(val zikir: Zikir?) : SheetKind
    data class VirdEdit(val vird: Vird?) : SheetKind
}

@Composable
fun MenuSheet(onDismiss: () -> Unit, go: (Scr) -> Unit, newZikir: () -> Unit) {
    val items = listOf(
        Triple(G.BEADS, "Tesbihat", "Sıralı zikir akışı") to { go(Scr.TESBIHAT) },
        Triple(G.BOOK, "Kütüphane", "Esmâ, salavat, günlük") to { go(Scr.KUTUPHANE) },
        Triple(G.CHART, "İstatistik", "Seri, takvim, grafik") to { go(Scr.ISTATISTIK) },
        Triple(G.FOCUS, "Odak Modu", "Sadece sayı, karanlık") to { go(Scr.ODAK) },
        Triple(G.PLUS, "Yeni Zikir", "Kendi zikrini ekle") to newZikir,
        Triple(G.GEAR, "Ayarlar", "Tema, titreşim, yedek") to { go(Scr.AYARLAR) },
    )
    LuxSheet("Menü", onDismiss) {
        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (t, action) -> MenuTile(t.first, t.second, t.third, Modifier.weight(1f)) { action() } }
            }
        }
    }
}

@Composable
private fun MenuTile(g: G, title: String, desc: String, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPal.current
    Column(
        modifier.glass(p, RoundedCornerShape(22.dp)).clickable(role = Role.Button, onClick = onClick).padding(16.dp),
    ) {
        Glyph(g, p.goldText, 26.dp)
        Spacer(Modifier.height(12.dp))
        Text(title, style = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = p.text))
        Text(desc, style = bodyStyle(p.sub).copy(fontSize = 12.sp, lineHeight = 16.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TargetSheet(initial: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial.toString()) }
    val v = text.toIntOrNull()
    val ok = v != null && v in 1..1_000_000
    LuxSheet("Hedef Belirle", onDismiss) {
        LuxField(text, { s -> text = s.filter { it.isDigit() }.take(7) }, "Hedef sayı", number = true)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(7, 11, 41, 300, 500, 1000).forEach { t -> GoldChip("$t", text == t.toString()) { text = t.toString() } }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Kaydet", Modifier.fillMaxWidth(), enabled = ok, glyph = G.CHECK) { onSave(v!!) }
    }
}

@Composable
fun EditZikirSheet(initial: Zikir?, onSave: (String, String, String, String, Int) -> Unit, onDismiss: () -> Unit) {
    var okunus by remember { mutableStateOf(initial?.okunus ?: "") }
    var arapca by remember { mutableStateOf(initial?.arapca ?: "") }
    var anlam by remember { mutableStateOf(initial?.anlam ?: "") }
    var fazilet by remember { mutableStateOf(initial?.fazilet ?: "") }
    var hedef by remember { mutableStateOf((initial?.target ?: 33).toString()) }
    val t = hedef.toIntOrNull()
    val ok = okunus.isNotBlank() && t != null && t in 1..1_000_000
    LuxSheet(if (initial == null) "Yeni Zikir" else "Zikri Düzenle", onDismiss) {
        Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LuxField(okunus, { okunus = it.take(80) }, "Zikir adı / okunuşu *")
            LuxField(arapca, { arapca = it.take(160) }, "Arapça yazılışı (isteğe bağlı)", rtl = true)
            LuxField(anlam, { anlam = it.take(160) }, "Anlamı (isteğe bağlı)", singleLine = false)
            LuxField(fazilet, { fazilet = it.take(200) }, "Not / fazilet (isteğe bağlı)", singleLine = false)
            LuxField(hedef, { s -> hedef = s.filter { it.isDigit() }.take(7) }, "Hedef", number = true)
        }
        Spacer(Modifier.height(18.dp))
        PrimaryButton("Kaydet", Modifier.fillMaxWidth(), enabled = ok, glyph = G.CHECK) {
            onSave(okunus.trim(), arapca.trim(), anlam.trim(), fazilet.trim(), t!!)
        }
    }
}

@Composable
fun VirdEditSheet(st: AppState, initial: Vird?, onDismiss: () -> Unit) {
    val p = LocalPal.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    val steps = remember { mutableStateListOf<Step>().apply { addAll(initial?.steps ?: emptyList()) } }
    var picking by remember { mutableStateOf(false) }
    val ok = name.isNotBlank() && steps.isNotEmpty()
    LuxSheet(if (initial == null) "Yeni Tesbihat" else "Tesbihatı Düzenle", onDismiss) {
        LuxField(name, { name = it.take(50) }, "Ad *")
        Spacer(Modifier.height(12.dp))
        Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
            steps.forEachIndexed { i, s ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp).glass(p, RoundedCornerShape(18.dp)).padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${i + 1}", style = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = p.goldText, fontFeatureSettings = NUM_FEATURES))
                    Spacer(Modifier.width(10.dp))
                    Text(s.okunus, style = bodyStyle(p.text), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    GlyphButton(G.LEFT, "Azalt", 32.dp, bordered = false) { steps[i] = s.copy(target = (s.target - if (s.target > 33) 10 else 1).coerceAtLeast(1)) }
                    Text(fmt(s.target), style = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = p.text, fontFeatureSettings = NUM_FEATURES, textAlign = TextAlign.Center), modifier = Modifier.widthIn(min = 40.dp))
                    GlyphButton(G.RIGHT, "Artır", 32.dp, bordered = false) { steps[i] = s.copy(target = s.target + if (s.target >= 33) 10 else 1) }
                    GlyphButton(G.UP, "Yukarı", 32.dp, bordered = false) { if (i > 0) { val tmp = steps[i - 1]; steps[i - 1] = s; steps[i] = tmp } }
                    GlyphButton(G.CLOSE, "Kaldır", 32.dp, bordered = false) { steps.removeAt(i) }
                }
            }
            if (picking) {
                Text("ZİKİR SEÇ", style = labelStyle(p.goldText), modifier = Modifier.padding(vertical = 8.dp))
                st.zikirs.forEach { z ->
                    Text(
                        z.okunus,
                        style = bodyStyle(p.text),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .clickable { steps.add(Step(z.arapca, z.okunus, z.anlam, z.target)); picking = false }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton(if (picking) "Vazgeç" else "Adım ekle", Modifier.weight(1f), if (picking) G.CLOSE else G.PLUS) { picking = !picking }
            PrimaryButton("Kaydet", Modifier.weight(1f), enabled = ok, glyph = G.CHECK) {
                st.saveVird(initial, name.trim(), steps.toList())
                onDismiss()
            }
        }
    }
}
