package com.virgo.cubetimer.ui.scramble

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.virgo.cubetimer.scramble.CubeState
import kotlin.math.min

/**
 * 立方体展开图（复刻 csTimer 的 scramble image）。
 *
 * 布局为 Kociemba 标准展开：
 * ```
 *       U
 *   L   F   R   B
 *       D
 * ```
 * 面序与 [CubeState.faceletsOf] 输出一致（`URFDLB`，每面 9 个字符按行优先）。
 * 配色为 csTimer 默认 `colcube`：白顶、绿前、红右、橙左、蓝后、黄底。
 */
@Composable
fun CubeNet(facelets: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (facelets.length < 54) return@Canvas

        val gap = 0.28f          // 面向间隙（单位：格）
        val unitsW = 4 * 3 + 3 * gap
        val unitsH = 3 * 3 + 2 * gap
        val cell = min(size.width / unitsW, size.height / unitsH)
        if (cell <= 0f) return@Canvas
        val ox = (size.width - unitsW * cell) / 2f
        val oy = (size.height - unitsH * cell) / 2f
        val faceStep = 3 * cell + gap * cell
        val inset = 0.06f * cell

        for (face in 0 until 6) {
            val col = FACE_COL[face]
            val row = FACE_ROW[face]
            val fx = ox + col * faceStep
            val fy = oy + row * faceStep
            // 底色：贴纸间的黑色分隔
            drawRect(
                color = Color(0xFF212121),
                topLeft = Offset(fx, fy),
                size = Size(3 * cell, 3 * cell),
            )
            for (r in 0 until 3) {
                for (c in 0 until 3) {
                    val ch = facelets[face * 9 + r * 3 + c]
                    drawRect(
                        color = faceColor(ch),
                        topLeft = Offset(fx + c * cell + inset, fy + r * cell + inset),
                        size = Size(cell - 2 * inset, cell - 2 * inset),
                    )
                }
            }
        }
    }
}

/** 面在展开图中的列（0..3），面序 U R F D L B。 */
private val FACE_COL = intArrayOf(1, 2, 1, 1, 0, 3)

/** 面在展开图中的行（0..2），面序 U R F D L B。 */
private val FACE_ROW = intArrayOf(0, 1, 1, 2, 1, 1)

/** csTimer 默认 `colcube` 配色：U 白、R 红、F 绿、D 黄、L 橙、B 蓝。 */
private fun faceColor(ch: Char): Color = when (ch) {
    'U' -> Color(0xFFFFFFFF)
    'R' -> Color(0xFFFF0000)
    'F' -> Color(0xFF00DD00)
    'D' -> Color(0xFFFFFF00)
    'L' -> Color(0xFFFFAA00)
    'B' -> Color(0xFF0000FF)
    else -> Color(0xFF888888)
}