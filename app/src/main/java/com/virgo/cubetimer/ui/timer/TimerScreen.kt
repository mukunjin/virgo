package com.virgo.cubetimer.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
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
 * 计时主界面：上方打乱文本区（含展开图与上一条/下一条）、中央大字 LCD、下方平均与会话信息。
 * 触摸语义复刻 csTimer：第一根手指按下即「按下」，全部松手才「松手」。
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
                        val pressedChanges = event.changes.filter { it.pressed }
                        if (pressedChanges.isEmpty()) {
                            // 全部手指抬起才松手（复刻 csTimer 的 touches.length === 0 判定）
                            if (down) {
                                down = false
                                pressed = false
                                vm.onRelease()
                            }
                        } else if (!down && pressedChanges.any { !it.isConsumed }) {
                            // 第一根未被子控件（左栏/浮窗按钮）消费的手指按下即开始
                            down = true
                            pressed = true
                            vm.onPress()
                        }
                    }
                }
            },
    ) {
        // 同时受高度与宽度约束：竖屏时按宽度收缩，避免大字溢出被裁切
        val lcdSize = min(maxHeight.value * 0.34f, maxWidth.value * 0.19f).sp
        val netHeight = (maxHeight.value * 0.2f).coerceIn(56f, 110f).dp

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = LeftBarWidth + 12.dp, end = 12.dp, top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ScrambleBar(
                    ui = ui,
                    onPrev = { vm.prevScramble() },
                    onNext = { vm.nextScramble() },
                    onRefresh = { vm.generateScramble() },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (ui.netFacelets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CubeNet(
                        facelets = ui.netFacelets,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(netHeight),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = displayText,
                fontSize = lcdSize,
                fontFamily = TimerFontFamily,
                fontWeight = FontWeight.Medium,
                color = lcdColor(ui.status, pressed, ui.useInspection),
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.weight(1f))

            SessionInfo(
                ui = ui,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            )
        }
    }
}

/** 左侧图标列宽度，供内容避让。 */
val LeftBarWidth = 64.dp

private fun lcdColor(status: TimerEngine.Status, pressed: Boolean, useInspection: Boolean) = when (status) {
    TimerEngine.Status.STOPPED -> VirgoColors.TimerRed
    TimerEngine.Status.READY -> if (pressed) VirgoColors.TimerGreen else VirgoColors.OnBackground
    TimerEngine.Status.INSPECTING -> if (pressed) VirgoColors.TimerYellow else VirgoColors.TimerRed
    TimerEngine.Status.READY_INSPECT -> if (pressed) VirgoColors.TimerGreen else VirgoColors.OnBackground
    TimerEngine.Status.RUNNING -> if (pressed) VirgoColors.TimerGreen else VirgoColors.OnBackground
    TimerEngine.Status.IDLE ->
        if (pressed) (if (useInspection) VirgoColors.TimerGreen else VirgoColors.TimerRed)
        else VirgoColors.OnBackground
}

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
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = ui.scramble.ifEmpty { if (ui.generating) "生成中…" else "" },
            color = VirgoColors.OnSurfaceVariant,
            fontSize = 18.sp,
            fontFamily = TimerFontFamily,
            textAlign = align,
            softWrap = ui.scrambleWrap,
            modifier = Modifier.weight(1f).padding(top = 4.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        NavButton(label = "◀", enabled = ui.canPrev, onClick = onPrev)
        NavButton(label = "▶", enabled = true, onClick = onNext)
        NavButton(label = "↻", enabled = !ui.generating, onClick = onRefresh)
    }
}

@Composable
private fun NavButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, VirgoColors.Border, RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (enabled) VirgoColors.OnSurfaceVariant else VirgoColors.Disabled,
        )
    }
}

@Composable
private fun SessionInfo(ui: TimerUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // 复刻 csTimer `#avgstr`：两行「ao5 / ao12」
        AvgLine("ao5", ui.stats.ao5, ui.useMilli)
        AvgLine("ao12", ui.stats.ao12, ui.useMilli)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${ui.sessionName.ifEmpty { "会话" }} · 共 ${ui.solves.size} 次",
            color = VirgoColors.OnSurfaceVariant,
            fontSize = 13.sp,
        )
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