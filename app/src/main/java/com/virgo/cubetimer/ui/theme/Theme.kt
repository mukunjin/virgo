package com.virgo.cubetimer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * Virgo 暗色配色：纯黑底 + 白色文字/按钮 + 深灰面板。
 * 保持原有字段命名，避免大面积改动调用点。
 */
object VirgoColors {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF141414)
    val SurfaceVariant = Color(0xFF262626)
    val Border = Color(0xFF3A3A3A)
    val OnBackground = Color(0xFFFFFFFF)
    val OnSurfaceVariant = Color(0xFF9E9E9E)
    val Disabled = Color(0xFF5A5A5A)

    /** 主按钮/选中态：白底黑字。 */
    val ButtonFill = Color(0xFFFFFFFF)
    val OnButton = Color(0xFF000000)

    /** 计时状态色，取值对齐 csTimer 的 `timerColors`。 */
    val TimerRed = Color(0xFFFF3B30)
    val TimerGreen = Color(0xFF32D74B)
    val TimerYellow = Color(0xFFFFD60A)
}

/** 大字 LCD 使用的等宽字体。 */
val TimerFontFamily: FontFamily = FontFamily.Monospace

private val VirgoDarkColors = darkColorScheme(
    primary = VirgoColors.ButtonFill,
    onPrimary = VirgoColors.OnButton,
    background = VirgoColors.Background,
    onBackground = VirgoColors.OnBackground,
    surface = VirgoColors.Background,
    onSurface = VirgoColors.OnBackground,
    surfaceVariant = VirgoColors.SurfaceVariant,
    onSurfaceVariant = VirgoColors.OnSurfaceVariant,
    outline = VirgoColors.Border,
)

@Composable
fun VirgoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = VirgoDarkColors, content = content)
}