@file:OptIn(ExperimentalMaterial3Api::class)

package com.gezgin.zikirmatik

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ───────────── Cam yüzey ─────────────
fun Modifier.glass(p: Pal, shape: Shape, strong: Boolean = false): Modifier = this
    .clip(shape)
    .background(
        Brush.linearGradient(
            if (strong) listOf(p.glassTop.copy(alpha = (p.glassTop.alpha * 1.6f).coerceAtMost(1f)), p.glassBottom.copy(alpha = (p.glassBottom.alpha * 1.6f).coerceAtMost(1f)))
            else listOf(p.glassTop, p.glassBottom)
        ),
        shape,
    )
    .border(
        1.dp,
        Brush.linearGradient(listOf(p.glassStroke, p.glassStroke.copy(alpha = p.glassStroke.alpha * 0.25f), p.gold.copy(alpha = 0.45f))),
        shape,
    )

// ───────────── Gökyüzü / zemin ─────────────
@Composable
fun Sky(p: Pal) {
    val breath = rememberInfiniteTransition(label = "nefes")
    val a by breath.animateFloat(0.75f, 1f, infiniteRepeatable(tween(9000), RepeatMode.Reverse), label = "a")
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(p.bgTop, p.bgMid, p.bgBottom)))
            fun glow(c: Color, x: Float, y: Float, r: Float) {
                val o = Offset(size.width * x, size.height * y)
                val rr = size.width * r
                drawCircle(Brush.radialGradient(listOf(c, Color.Transparent), center = o, radius = rr), radius = rr, center = o)
            }
            glow(p.glowB, -0.05f, 0.50f, 0.70f)
            glow(p.glowC, 1.05f, 0.78f, 0.75f)
            if (p.stars > 0f) {
                val rnd = java.util.Random(19)
                repeat(90) {
                    val x = rnd.nextFloat() * size.width
                    val y = rnd.nextFloat() * size.height * 0.7f
                    val big = rnd.nextFloat() > 0.93f
                    val r = (if (big) 1.1f else 0.45f + rnd.nextFloat() * 0.6f) * density
                    val al = (0.10f + rnd.nextFloat() * 0.40f) * p.stars * (1f - y / (size.height * 0.75f))
                    drawCircle(Color.White.copy(alpha = al.coerceIn(0f, 1f)), r, Offset(x, y))
                    if (big) drawCircle(Color.White.copy(alpha = 0.10f * p.stars), r * 3.5f, Offset(x, y))
                }
            }
        }
        // Yavaşça "nefes alan" üst ışık
        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = a }) {
            val o = Offset(size.width * 0.5f, -size.height * 0.06f)
            val rr = size.width * 1.05f
            drawCircle(Brush.radialGradient(listOf(p.glowA, Color.Transparent), center = o, radius = rr), radius = rr, center = o)
        }
    }
}

// ───────────── Süs çizgisi ─────────────
@Composable
fun Ornament(width: Dp = 150.dp, alpha: Float = 1f) {
    val p = LocalPal.current
    Canvas(Modifier.width(width).height(10.dp).graphicsLayer { this.alpha = alpha }) {
        val cy = size.height / 2
        val cx = size.width / 2
        val gap = 12.dp.toPx()
        val sw = 0.8.dp.toPx()
        drawLine(Brush.horizontalGradient(listOf(Color.Transparent, p.gold), 0f, cx - gap), Offset(0f, cy), Offset(cx - gap, cy), sw)
        drawLine(Brush.horizontalGradient(listOf(p.gold, Color.Transparent), cx + gap, size.width), Offset(cx + gap, cy), Offset(size.width, cy), sw)
        val d = 4.dp.toPx()
        drawPath(Path().apply { moveTo(cx, cy - d); lineTo(cx + d, cy); lineTo(cx, cy + d); lineTo(cx - d, cy); close() }, p.goldBrush)
        drawCircle(p.gold, 1.2.dp.toPx(), Offset(cx - gap + 4.dp.toPx(), cy))
        drawCircle(p.gold, 1.2.dp.toPx(), Offset(cx + gap - 4.dp.toPx(), cy))
    }
}

/** Sekiz köşeli yıldız logosu (açılış ve başlıklar). */
@Composable
fun StarLogo(size: Dp, modifier: Modifier = Modifier) {
    val p = LocalPal.current
    Canvas(modifier.size(size)) {
        val c = center
        val h = this.size.minDimension * 0.30f
        val tl = Offset(c.x - h, c.y - h)
        val sq = androidx.compose.ui.geometry.Size(h * 2, h * 2)
        drawRect(p.goldBrush, tl, sq)
        rotate(45f, c) { drawRect(p.goldBrush, tl, sq) }
        drawCircle(p.bgMid, h * 0.52f, c)
        drawCircle(p.goldLight, h * 0.22f, c)
        drawCircle(p.gold.copy(alpha = 0.5f), this.size.minDimension * 0.47f, c, style = Stroke(1.dp.toPx()))
    }
}

// ───────────── Düğmeler ─────────────
@Composable
fun GlyphButton(g: G, desc: String, size: Dp = 44.dp, bordered: Boolean = true, tint: Color? = null, onClick: () -> Unit) {
    val p = LocalPal.current
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .then(if (bordered) Modifier.glass(p, CircleShape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        Glyph(g, tint ?: p.goldText, size * 0.48f)
    }
}

@Composable
fun GoldChip(label: String, selected: Boolean, glyph: G? = null, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalPal.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .then(if (selected) Modifier.background(p.goldBrush) else Modifier.border(1.dp, p.gold.copy(alpha = 0.45f), shape))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (glyph != null) {
            Glyph(glyph, if (selected) p.onGold else p.goldText, 14.dp)
            Spacer(Modifier.width(5.dp))
        }
        Text(
            label,
            style = TextStyle(
                fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.6.sp,
                color = if (selected) p.onGold else p.text, fontFeatureSettings = NUM_FEATURES,
            ),
        )
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, glyph: G? = null, onClick: () -> Unit) {
    val p = LocalPal.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .then(if (enabled) Modifier.background(p.goldBrush) else Modifier.border(1.dp, p.faint, shape))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (glyph != null) { Glyph(glyph, if (enabled) p.onGold else p.faint, 16.dp); Spacer(Modifier.width(8.dp)) }
        Text(text, style = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.8.sp, color = if (enabled) p.onGold else p.faint))
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, glyph: G? = null, onClick: () -> Unit) {
    val p = LocalPal.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .border(1.dp, p.gold.copy(alpha = 0.5f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (glyph != null) { Glyph(glyph, p.goldText, 16.dp); Spacer(Modifier.width(8.dp)) }
        Text(text, style = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.6.sp, color = p.text))
    }
}

// ───────────── Anahtar (switch) ─────────────
@Composable
fun LuxSwitch(on: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPal.current
    val x by animateDpAsState(if (on) 20.dp else 0.dp, label = "sw")
    val knob by animateColorAsState(if (on) p.onGold else p.sub, label = "knob")
    Box(
        Modifier
            .size(48.dp, 28.dp)
            .clip(RoundedCornerShape(50))
            .then(if (on) Modifier.background(p.goldBrush) else Modifier.border(1.dp, p.faint, RoundedCornerShape(50)))
            .clickable(role = Role.Switch) { onChange(!on) }
            .padding(4.dp),
    ) {
        Box(Modifier.offset(x = x).size(20.dp).clip(CircleShape).background(knob))
    }
}

// ───────────── Form alanı ─────────────
@Composable
fun LuxField(value: String, onChange: (String) -> Unit, label: String, number: Boolean = false, rtl: Boolean = false, singleLine: Boolean = true) {
    val p = LocalPal.current
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontFamily = Sans) },
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 3,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        textStyle = TextStyle(
            color = p.text,
            fontFamily = if (rtl) Arabic else Sans,
            fontSize = if (rtl) 22.sp else 16.sp,
            textDirection = if (rtl) TextDirection.Rtl else TextDirection.Content,
        ),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = p.gold,
            unfocusedBorderColor = p.gold.copy(alpha = 0.35f),
            focusedLabelColor = p.goldText,
            unfocusedLabelColor = p.sub,
            cursorColor = p.gold,
            focusedTextColor = p.text,
            unfocusedTextColor = p.text,
            focusedContainerColor = p.glassTop,
            unfocusedContainerColor = p.glassBottom,
        ),
    )
}

// ───────────── Bölüm başlığı / satırlar ─────────────
@Composable
fun SectionTitle(text: String) {
    val p = LocalPal.current
    Text(text.uppercase(java.util.Locale.forLanguageTag("tr")), style = labelStyle(p.goldText), modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 10.dp))
}

@Composable
fun SettingRow(title: String, desc: String? = null, trailing: @Composable () -> Unit) {
    val p = LocalPal.current
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = p.text))
            if (desc != null) {
                Spacer(Modifier.height(2.dp))
                Text(desc, style = bodyStyle(p.sub).copy(fontSize = 12.5.sp, lineHeight = 17.sp))
            }
        }
        trailing()
    }
}

@Composable
fun Stepper(value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    val p = LocalPal.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        GlyphButton(G.LEFT, "Azalt", 36.dp, onClick = onMinus)
        Text(
            value,
            style = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = p.text, fontFeatureSettings = NUM_FEATURES, textAlign = TextAlign.Center),
            modifier = Modifier.widthIn(min = 72.dp),
        )
        GlyphButton(G.RIGHT, "Artır", 36.dp, onClick = onPlus)
    }
}

// ───────────── Ekran çerçevesi ─────────────
@Composable
fun ScreenFrame(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPal.current
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlyphButton(G.BACK, "Geri", onClick = onBack)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = titleStyle(p.numberBrush).copy(fontSize = 24.sp))
                Spacer(Modifier.height(2.dp))
                Ornament(80.dp)
            }
            if (action != null) action() else Spacer(Modifier.size(44.dp))
        }
        content()
    }
}

// ───────────── Alt panel (bottom sheet) ─────────────
@Composable
fun LuxSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPal.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = p.sheet,
        contentColor = p.text,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        scrimColor = Color.Black.copy(alpha = if (p.dark) 0.6f else 0.35f),
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 4.dp).size(40.dp, 4.dp).clip(RoundedCornerShape(50)).background(p.gold.copy(alpha = 0.5f)))
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = titleStyle(p.numberBrush).copy(fontSize = 23.sp))
            Spacer(Modifier.height(6.dp))
            Ornament(100.dp)
            Spacer(Modifier.height(18.dp))
            content()
        }
    }
}

// ───────────── Bildirim çubuğu ─────────────
@Composable
fun LuxSnack(data: SnackbarData) {
    val p = LocalPal.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(p.sheet)
            .border(1.dp, p.sheetLine, shape)
            .padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(data.visuals.message, style = bodyStyle(p.text), modifier = Modifier.weight(1f).padding(vertical = 8.dp))
        val action = data.visuals.actionLabel
        if (action != null) {
            Text(
                action.uppercase(java.util.Locale.forLanguageTag("tr")),
                style = labelStyle(p.goldText).copy(fontSize = 11.sp),
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable { data.performAction() }.padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
}
