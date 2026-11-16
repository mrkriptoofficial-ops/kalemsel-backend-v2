package com.gezgin.zikirmatik

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Uygulamaya özel, aynı çizgi kalınlığında ince ikon seti (24 birimlik ızgara). */
enum class G { LIST, PLUS, GRID, UNDO, RESET, CHART, FOCUS, GEAR, BOOK, BEADS, CLOSE, EDIT, TRASH, UP, DOWN, LEFT, RIGHT, CHECK, BELL, BACK, PLAY, EXPORT, IMPORT, SKIP, MINUS, GRIP }

@Composable
fun Glyph(g: G, tint: Color, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    // Yön bildiren ikonlar sağdan sola dillerde aynalanır
    val mirror = LocalLayoutDirection.current == LayoutDirection.Rtl &&
        g in setOf(G.LEFT, G.RIGHT, G.BACK, G.UNDO, G.SKIP, G.PLAY)
    Canvas(modifier.size(size).graphicsLayer { if (mirror) scaleX = -1f }) {
        val s = this.size.minDimension / 24f
        val sw = 1.6f * s
        val st = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun pt(x: Float, y: Float) = Offset(x * s, y * s)
        fun ln(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(tint, pt(x1, y1), pt(x2, y2), sw, cap = StrokeCap.Round)
        fun path(block: Path.() -> Unit) = drawPath(Path().apply(block), tint, style = st)
        fun Path.m(x: Float, y: Float) = moveTo(x * s, y * s)
        fun Path.l(x: Float, y: Float) = lineTo(x * s, y * s)
        fun Path.c(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) = cubicTo(x1 * s, y1 * s, x2 * s, y2 * s, x3 * s, y3 * s)
        fun ring(cx: Float, cy: Float, r: Float) = drawCircle(tint, r * s, pt(cx, cy), style = st)

        when (g) {
            G.LIST -> { ln(4f, 7f, 20f, 7f); ln(4f, 12f, 20f, 12f); ln(4f, 17f, 13f, 17f) }
            G.PLUS -> { ln(12f, 5f, 12f, 19f); ln(5f, 12f, 19f, 12f) }
            G.GRID -> listOf(4f to 4f, 13.5f to 4f, 4f to 13.5f, 13.5f to 13.5f).forEach { (x, y) ->
                drawRoundRect(tint, pt(x, y), Size(6.5f * s, 6.5f * s), CornerRadius(2f * s), style = st)
            }
            G.UNDO -> {
                path { m(8.5f, 6.5f); l(4.5f, 10.5f); l(8.5f, 14.5f) }
                path { m(4.5f, 10.5f); l(14f, 10.5f); c(17.3f, 10.5f, 19.5f, 12.7f, 19.5f, 15f); c(19.5f, 17.3f, 17.3f, 19f, 14f, 19f); l(10f, 19f) }
            }
            G.RESET -> {
                drawArc(tint, -50f, 290f, false, pt(5f, 5f), Size(14f * s, 14f * s), style = st)
                path { m(16.4f, 3.6f); l(16.6f, 7.3f); l(20.2f, 6.6f) }
            }
            G.CHART -> { ln(4.5f, 19.5f, 19.5f, 19.5f); ln(8f, 16f, 8f, 11f); ln(12f, 16f, 12f, 6f); ln(16f, 16f, 16f, 9f) }
            G.FOCUS -> {
                path { m(4.5f, 9f); l(4.5f, 4.5f); l(9f, 4.5f) }
                path { m(15f, 4.5f); l(19.5f, 4.5f); l(19.5f, 9f) }
                path { m(19.5f, 15f); l(19.5f, 19.5f); l(15f, 19.5f) }
                path { m(9f, 19.5f); l(4.5f, 19.5f); l(4.5f, 15f) }
                ring(12f, 12f, 2.6f)
            }
            G.GEAR -> {
                ring(12f, 12f, 3f)
                ring(12f, 12f, 6.2f)
                for (k in 0 until 8) {
                    val a = Math.toRadians(k * 45.0)
                    ln(12f + 6.2f * cos(a).toFloat(), 12f + 6.2f * sin(a).toFloat(), 12f + 8.6f * cos(a).toFloat(), 12f + 8.6f * sin(a).toFloat())
                }
            }
            G.BOOK -> {
                path { m(12f, 7f); c(9.5f, 5.3f, 6.8f, 5f, 4f, 5.8f); l(4f, 18f); c(6.8f, 17.2f, 9.5f, 17.5f, 12f, 19.2f) }
                path { m(12f, 7f); c(14.5f, 5.3f, 17.2f, 5f, 20f, 5.8f); l(20f, 18f); c(17.2f, 17.2f, 14.5f, 17.5f, 12f, 19.2f) }
                ln(12f, 7f, 12f, 19.2f)
            }
            G.BEADS -> {
                for (k in 0 until 9) {
                    val a = Math.toRadians(-90.0 + 40.0 * k + 20.0)
                    drawCircle(tint, 1.35f * s, pt(12f + 6.3f * cos(a).toFloat(), 10.5f + 6.3f * sin(a).toFloat()), style = st)
                }
                ln(12f, 16.8f, 12f, 21f)
            }
            G.CLOSE -> { ln(6.5f, 6.5f, 17.5f, 17.5f); ln(17.5f, 6.5f, 6.5f, 17.5f) }
            G.EDIT -> {
                path { m(5f, 19f); l(5.6f, 15.4f); l(15.2f, 5.8f); l(18.2f, 8.8f); l(8.6f, 18.4f); l(5f, 19f) }
                ln(13.2f, 7.8f, 16.2f, 10.8f)
            }
            G.TRASH -> {
                ln(4.5f, 7f, 19.5f, 7f)
                path { m(6.8f, 7f); l(7.8f, 19.5f); l(16.2f, 19.5f); l(17.2f, 7f) }
                ln(10f, 4.5f, 14f, 4.5f); ln(10.2f, 10.5f, 10.4f, 16f); ln(13.8f, 10.5f, 13.6f, 16f)
            }
            G.UP -> path { m(6f, 15f); l(12f, 9f); l(18f, 15f) }
            G.DOWN -> path { m(6f, 9f); l(12f, 15f); l(18f, 9f) }
            G.LEFT -> path { m(15f, 5.5f); l(8.5f, 12f); l(15f, 18.5f) }
            G.RIGHT -> path { m(9f, 5.5f); l(15.5f, 12f); l(9f, 18.5f) }
            G.CHECK -> path { m(5f, 12.5f); l(10f, 17.2f); l(19f, 7.2f) }
            G.BELL -> {
                path { m(6f, 16.5f); l(18f, 16.5f); l(16.6f, 14.2f); l(16.6f, 10.6f); c(16.6f, 7.6f, 14.6f, 5.4f, 12f, 5.4f); c(9.4f, 5.4f, 7.4f, 7.6f, 7.4f, 10.6f); l(7.4f, 14.2f); l(6f, 16.5f) }
                ln(10.5f, 19.5f, 13.5f, 19.5f)
            }
            G.BACK -> { path { m(10.5f, 6f); l(4.5f, 12f); l(10.5f, 18f) }; ln(4.5f, 12f, 19.5f, 12f) }
            G.PLAY -> path { m(8f, 5.5f); l(18.5f, 12f); l(8f, 18.5f); l(8f, 5.5f) }
            G.EXPORT -> { ln(12f, 4f, 12f, 15f); path { m(7.5f, 10.5f); l(12f, 15f); l(16.5f, 10.5f) }; ln(5f, 19.5f, 19f, 19.5f) }
            G.IMPORT -> { ln(12f, 15f, 12f, 4f); path { m(7.5f, 8.5f); l(12f, 4f); l(16.5f, 8.5f) }; ln(5f, 19.5f, 19f, 19.5f) }
            G.MINUS -> ln(5f, 12f, 19f, 12f)
            G.GRIP -> listOf(8f, 12f, 16f).forEach { y -> drawCircle(tint, 1.3f * s, pt(9.5f, y)); drawCircle(tint, 1.3f * s, pt(14.5f, y)) }
            G.SKIP -> { path { m(6f, 6f); l(13f, 12f); l(6f, 18f) }; ln(17f, 6f, 17f, 18f) }
        }
    }
}
