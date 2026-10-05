package com.virgo.cubetimer.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virgo.cubetimer.timer.TimeFormat
import com.virgo.cubetimer.timer.TimerEngine
import com.virgo.cubetimer.ui.scramble.CubeNet
import com.virgo.cubetimer.ui.theme.TimerFontFamily
import com.virgo.cubetimer.ui.theme.VirgoColors
import kotlin.math.min

/**
 * 计时主界面：顶部打乱区（打乱文本 + 上一条/下一条/刷新）、
 * 中央大字 LCD、下方 ao5/ao12，打乱展开图固定在右下角。
 * 触摸语义复刻 csTimer：第一根手指按下即「按下」，全部松手才「松手」，全屏任意位置均可计时。
 */
@Composable
fun TimerScreen(ui: TimerUiState, vm: TimerViewModel, modifier: Modifier = Modifier) {
    var pressed by remember { mutableStateOf(false) }
    var tickMs by remember { mutableLongStateOf(0L) }

    val ticking = ui.status == TimerEngine.Status.RUNNING ||
        ui.status == TimerEngine.Status.INSPECTING

    // 运行/观察时每帧刷新（等价 csTimer 的 requestAnimFrame 循环）
    LaunchedEffect(ui.status) {
        while (ticking) {
            androidx.compose.runtime.withFrameNanos { tickMs = vm.elapsedMs() }
        }
    }

    val displayText = remember(ui.status, tickMs) { vm.displayText() }

    BoxWithConstraints(
        modifier = modifier
            .background(VirgoColors.Background)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var down = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val active = event.changes.filter { it.pressed }
                        if (active.isEmpty()) {
                            // 全部手指抬起才松手（复刻 csTimer 的 touches.length === 0 判定）
                            if (down) {
                                down = false
                                pressed = false
                                vm.onRelease()
                            }
                        } else if (!down) {
                            down = true
                            pressed = true
                            vm.onPress()
                        }
                    }
                }
            },
    ) {
        val landscape = maxWidth > maxHeight
        // 以屏幕短边统一定标（横屏的 maxHeight、竖屏的 maxWidth 都是短边）：
        // 竖屏系数大于横屏，从而保证「竖屏字号 > 横屏字号」；同时按短边缩放，避免单行时间溢出
        val shortSide = min(maxWidth.value, maxHeight.value)
        val base = shortSide * if (landscape) 0.23f else 0.25f
        // 再按当前文本长度收缩：等宽字体单字符约占 0.62em。
        // 计时超过一分钟位数变多，据此收缩可确保任何长度都不会横向溢出。
        val chars = displayText.length.coerceAtLeast(5)
        val fitByWidth = (maxWidth.value - 32f) / (chars * 0.62f)
        val lcdSize = min(base, fitByWidth).sp

        // 从进入观察到拍表（含观察、就绪、计时中）之间，屏幕上只保留时间
        val focus = ui.status.hidesOtherUi

        val density = LocalDensity.current
        val lcdHeight = with(density) { lcdSize.toPx().toDp() }

        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部打乱区（全屏均可计时）。观察开始到拍表之间只留时间，其余元素隐去
            if (!focus) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                ) {
                    ScrambleBar(
                        ui = ui,
                        onPrev = { vm.prevScramble() },
                        onNext = { vm.nextScramble() },
                        onRefresh = { vm.generateScramble() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            SevenSegmentDisplay(
                text = displayText,
                color = lcdColor(ui.status, pressed, ui.useInspection),
                digitHeight = lcdHeight,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.weight(1f))

            if (!focus) {
                SessionInfo(
                    ui = ui,
                    modifier = Modifier
                        .fillMaxWidth()
                        // 底部为胶囊切换栏留出空间，避免遮挡 ao5/ao12
                        .padding(bottom = 84.dp),
                )
            }
        }

        // 打乱展开图固定到右下角（计时聚焦时一并隐去）
        if (!focus && ui.netFacelets.isNotEmpty()) {
            CubeNet(
                facelets = ui.netFacelets,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 84.dp)
                    .size(if (landscape) 132.dp else 104.dp),
            )
        }
    }
}

/**
 * 从开始观察到拍表之间：观察中、就绪、计时中三种状态都只显示时间，
 * 打乱文本、◀ ▶ ↻、ao5/ao12、打乱展开图与底部胶囊一并隐去；拍表（STOPPED）后恢复。
 */
internal val TimerEngine.Status.hidesOtherUi: Boolean
    get() = this == TimerEngine.Status.INSPECTING ||
        this == TimerEngine.Status.READY ||
        this == TimerEngine.Status.RUNNING

private fun lcdColor(status: TimerEngine.Status, pressed: Boolean, useInspection: Boolean) = when (status) {
    TimerEngine.Status.STOPPED -> VirgoColors.TimerRed
    TimerEngine.Status.READY -> if (pressed) VirgoColors.TimerGreen else VirgoColors.OnBackground
    TimerEngine.Status.INSPECTING -> if (pressed) VirgoColors.TimerYellow else VirgoColors.TimerRed
    TimerEngine.Status.RUNNING -> if (pressed) VirgoColors.TimerGreen else VirgoColors.OnBackground
    TimerEngine.Status.IDLE ->
        if (pressed) (if (useInspection) VirgoColors.TimerGreen else VirgoColors.TimerRed)
        else VirgoColors.OnBackground
}

/** 打乱区：打乱文本在上，`◀ ▶ ↻` 在下方居中（竖屏/横屏一致）。 */
@Composable
private fun ScrambleBar(
    ui: TimerUiState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val align = when (ui.scrambleAlign) {
        0 -> TextAlign.Start
        2 -> TextAlign.End
        else -> TextAlign.Center
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = ui.scramble.ifEmpty { if (ui.generating) "生成中…" else "" },
            color = VirgoColors.OnSurfaceVariant,
            fontSize = 18.sp,
            fontFamily = TimerFontFamily,
            textAlign = align,
            softWrap = ui.scrambleWrap,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NavButton(label = "◀", enabled = ui.canPrev, onClick = onPrev)
            NavButton(label = "▶", enabled = true, onClick = onNext)
            NavButton(label = "↻", enabled = !ui.generating, onClick = onRefresh)
        }
    }
}

/** 药丸按钮：Material 全圆角，比原方形略大，便于点按。 */
@Composable
private fun NavButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 40.dp)
            .clip(shape)
            .background(if (enabled) VirgoColors.ButtonFill else VirgoColors.Surface)
            .border(1.dp, VirgoColors.Border, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = if (enabled) VirgoColors.OnButton else VirgoColors.Disabled,
        )
    }
}

/** 底部统计：仅保留 ao5 / ao12。 */
@Composable
private fun SessionInfo(ui: TimerUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AvgLine("ao5", ui.stats.ao5, ui.useMilli)
        AvgLine("ao12", ui.stats.ao12, ui.useMilli)
    }
}

@Composable
private fun AvgLine(label: String, value: Double?, useMilli: Boolean) {
    Text(
        text = "$label: ${TimeFormat.prettyAvgOrDnf(value, useMilli)}",
        color = VirgoColors.OnSurfaceVariant,
        fontSize = 15.sp,
        fontFamily = TimerFontFamily,
    )
}