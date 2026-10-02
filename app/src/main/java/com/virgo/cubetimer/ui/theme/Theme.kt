package com.virgo.cubetimer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * Virgo 白 + 灰配色（替代 csTimer 默认绿色 #EEFFCC）。
 * 不引入彩色，强调态统一用深浅灰。
 */
object VirgoColors {
    val Background = Color(0xFFFFFFFF)
    val Surface = Color(0xFFF5F5F5)
    val SurfaceVariant = Color(0xFFEEEEEE)
    val Border = Color(0xFFE0E0E0)
    val OnBackground = Color(0xFF212121)
    val OnSurfaceVariant = Color(0xFF757575)
    val Disabled = Color(0xFFBDBDBD)

    /** 计时状态色，取值对齐 csTimer 的 `timerColors`。 */
    val TimerRed = Color(0xFFFF0000)
    val TimerGreen = Color(0xFF00DD00)
    val TimerYellow = Color(0xFFDDDD00)
}

/** 大字 LCD 使用的等宽字体。 */
val TimerFontFamily: FontFamily = FontFamily.Monospace

private val VirgoLightColors = lightColorScheme(
    primary = VirgoColors.OnBackground,
    onPrimary = VirgoColors.Background,
    background = VirgoColors.Background,
    onBackground = VirgoColors.OnBackground,
    surface = VirgoColors.Background,
    onSurface = VirgoColors.OnBackground,
    surfaceVariant = VirgoColors.Surface,
    onSurfaceVariant = VirgoColors.OnSurfaceVariant,
    outline = VirgoColors.Border,
)

@Composable
fun VirgoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = VirgoLightColors, content = content)
}