package com.virgo.cubetimer.ui.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import kotlin.math.hypot

/** 小数点后的字符相对小数点前的高度比例（三分之二）。 */
private const val DECIMAL_SCALE = 2f / 3f

/** 数码管的字宽系数（字宽 = 字高 * 该值）。 */
private const val GLYPH_WIDTH = 0.6f

/** 分隔符（'.' / ':'）的占位宽度系数。 */
private const val NARROW_WIDTH = 0.3f

/** 相邻字符之间的间距系数（间距 = 字高 * 该值）。 */
private const val GAP = 0.09f

// 七段段码位标志
private const val SEG_A = 1
private const val SEG_B = 2
private const val SEG_C = 4
private const val SEG_D = 8
private const val SEG_E = 16
private const val SEG_F = 32
private const val SEG_G = 64

/**
 * 七段数码管样式的数字显示。
 *
 * 逐字符绘制：数字 0-9 使用标准七段段码，字母 D/N/F 与 '-'、'+'、'.'、':' 用相近形状绘制。
 * [text] 中位于小数点之后的字符高度为小数点前的 [DECIMAL_SCALE]（2/3），
 * 各字符以底部对齐，整体在可用宽度内水平居中。
 */
@Composable
fun SevenSegmentDisplay(
    text: String,
    color: Color,
    digitHeight: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(digitHeight)) {
        if (text.isEmpty()) return@Canvas
        val full = size.height
        val stroke = full * 0.10f
        val gap = full * GAP
        val dotIndex = text.indexOf('.')

        fun heightOf(i: Int): Float =
            if (dotIndex >= 0 && i > dotIndex) full * DECIMAL_SCALE else full

        fun widthOf(c: Char, h: Float): Float = when (c) {
            '.', ':' -> h * NARROW_WIDTH
            else -> h * GLYPH_WIDTH
        }

        val widths = FloatArray(text.length) { i -> widthOf(text[i], heightOf(i)) }
        val total = widths.sum() + gap * (text.length - 1).coerceAtLeast(0)
        var x = (size.width - total) / 2f

        text.forEachIndexed { i, c ->
            val h = heightOf(i)
            // 底部对齐
            drawGlyph(c, x, full - h, h, widths[i], color, stroke)
            x += widths[i] + gap
        }
    }
}

/**
 * 返回 [text] 用七段管绘制时所需的总宽度（以整字高为单位）。
 * 用于按可用宽度精确反推字号，避免放大后横向溢出。
 */
fun sevenSegmentWidthFactor(text: String): Float {
    if (text.isEmpty()) return 0f
    val dotIndex = text.indexOf('.')
    var w = 0f
    text.forEachIndexed { i, c ->
        val h = if (dotIndex >= 0 && i > dotIndex) DECIMAL_SCALE else 1f
        val cw = if (c == '.' || c == ':') NARROW_WIDTH else GLYPH_WIDTH
        w += cw * h
    }
    w += GAP * (text.length - 1).coerceAtLeast(0)
    return w
}

private fun DrawScope.drawGlyph(
    c: Char,
    x: Float,
    y: Float,
    h: Float,
    cellWidth: Float,
    color: Color,
    stroke: Float,
) {
    val cx = x + cellWidth / 2f
    when (c) {
        '.' -> drawDot(cx, y + h - stroke, color, stroke)
        ':' -> {
            drawDot(cx, y + h * 0.32f, color, stroke)
            drawDot(cx, y + h * 0.68f, color, stroke)
        }
        '+' -> {
            drawSeg(x + stroke, y + h / 2f, x + cellWidth - stroke, y + h / 2f, color, stroke)
            drawSeg(cx, y + stroke, cx, y + h - stroke, color, stroke)
        }
        else -> {
            val mask = SEGMENT_MAP[c]
            if (mask != null) drawSegments(mask, x, y, cellWidth, h, color, stroke)
        }
    }
}

/** 绘制一个点（实心圆点）。 */
private fun DrawScope.drawDot(cx: Float, cy: Float, color: Color, stroke: Float) {
    drawCircle(color, radius = stroke * 0.75f, center = Offset(cx, cy))
}

/** 绘制一条段：两端尖的六边形（真实数码管的段形状）。 */
private fun DrawScope.drawSeg(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, stroke: Float) {
    val dx = x2 - x1
    val dy = y2 - y1
    val len = hypot(dx, dy)
    if (len <= 0.0001f) return
    val ux = dx / len
    val uy = dy / len
    val half = stroke / 2f
    // 尖角沿段方向延伸的长度（约 45°）
    val tip = half
    // 段方向的法向量，用于撑开段厚度
    val hx = -uy * half
    val hy = ux * half
    val path = Path().apply {
        moveTo(x1, y1)
        lineTo(x1 + ux * tip + hx, y1 + uy * tip + hy)
        lineTo(x2 - ux * tip + hx, y2 - uy * tip + hy)
        lineTo(x2, y2)
        lineTo(x2 - ux * tip - hx, y2 - uy * tip - hy)
        lineTo(x1 + ux * tip - hx, y1 + uy * tip - hy)
        close()
    }
    drawPath(path, color)
}

/** 按段码绘制一个字符。 */
private fun DrawScope.drawSegments(
    mask: Int,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    color: Color,
    stroke: Float,
) {
    val pad = stroke * 0.5f
    val l = x + pad
    val r = x + w - pad
    val t = y + pad
    val b = y + h - pad
    val m = y + h / 2f
    fun on(seg: Int) = mask and seg != 0
    if (on(SEG_A)) drawSeg(l, t, r, t, color, stroke)
    if (on(SEG_B)) drawSeg(r, t, r, m, color, stroke)
    if (on(SEG_C)) drawSeg(r, m, r, b, color, stroke)
    if (on(SEG_D)) drawSeg(l, b, r, b, color, stroke)
    if (on(SEG_E)) drawSeg(l, m, l, b, color, stroke)
    if (on(SEG_F)) drawSeg(l, t, l, m, color, stroke)
    if (on(SEG_G)) drawSeg(l, m, r, m, color, stroke)
}

/** 数字与可映射字符的七段段码。未列出的字符不绘制。 */
private val SEGMENT_MAP: Map<Char, Int> = mapOf(
    '0' to (SEG_A or SEG_B or SEG_C or SEG_D or SEG_E or SEG_F),
    '1' to (SEG_B or SEG_C),
    '2' to (SEG_A or SEG_B or SEG_G or SEG_E or SEG_D),
    '3' to (SEG_A or SEG_B or SEG_G or SEG_C or SEG_D),
    '4' to (SEG_F or SEG_G or SEG_B or SEG_C),
    '5' to (SEG_A or SEG_F or SEG_G or SEG_C or SEG_D),
    '6' to (SEG_A or SEG_F or SEG_G or SEG_E or SEG_C or SEG_D),
    '7' to (SEG_A or SEG_B or SEG_C),
    '8' to (SEG_A or SEG_B or SEG_C or SEG_D or SEG_E or SEG_F or SEG_G),
    '9' to (SEG_A or SEG_B or SEG_C or SEG_D or SEG_F or SEG_G),
    // 字母近似：D、F 为通用写法；N 用 7 段的 n 形（C/E/G）近似
    'D' to (SEG_B or SEG_C or SEG_D or SEG_E or SEG_G),
    'F' to (SEG_A or SEG_E or SEG_F or SEG_G),
    'N' to (SEG_C or SEG_E or SEG_G),
    '-' to SEG_G,
)