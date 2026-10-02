package com.virgo.cubetimer.ui.menu

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virgo.cubetimer.ui.theme.VirgoColors
import kotlin.math.abs
import kotlin.math.roundToInt

/** 底部分段胶囊可切换的四个界面。 */
enum class Panel(val title: String) {
    TIMER("计时"),
    STATS("成绩"),
    SETTINGS("设置"),
    ABOUT("关于"),
}

private val ThumbHeight = 50.dp
private val BarHeight = 58.dp
private val ThumbGap = 10.dp

/**
 * 底部分段胶囊：深灰轨道 + 白色实心指示块。
 *
 * 位置用「归一化比例 0..1」表达（与轨道像素宽度无关，横竖屏切换天然不会越界）；
 * 动画完全交给声明式动画 [animateFloatAsState]，不手写协程，因此不存在
 * 「换了界面导致重组、动画没跑起来」的时序竞态。
 * 拖动时用 snap 瞬时跟手，点击/松手时用 tween 平滑滑向目标段中心。
 */
@Composable
fun CapsuleBar(current: Panel, onSelect: (Panel) -> Unit, modifier: Modifier = Modifier) {
    val items = Panel.entries
    val count = items.size
    val density = LocalDensity.current

    val trackWidth = remember { mutableFloatStateOf(0f) }
    /** 拖动中的指示块中心比例（0..1）；为 null 表示未拖动，位置由 [current] 决定。 */
    var dragFrac by remember { mutableStateOf<Float?>(null) }

    fun fracOf(panel: Panel) = (items.indexOf(panel) + 0.5f) / count

    val dragging = dragFrac != null
    val targetFrac = dragFrac ?: fracOf(current)
    val frac by animateFloatAsState(
        targetValue = targetFrac,
        animationSpec = if (dragging) snap() else tween(durationMillis = 280),
        label = "capsuleThumb",
    )

    val trackWidthPx = trackWidth.floatValue
    val segWidthPx = if (trackWidthPx > 0f) trackWidthPx / count else 0f
    val gapPx = with(density) { ThumbGap.toPx() }
    val thumbWidthPx = (segWidthPx - gapPx).coerceAtLeast(0f)

    Box(
        modifier = modifier
            .height(BarHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(VirgoColors.SurfaceVariant)
            .border(1.dp, VirgoColors.Border, RoundedCornerShape(percent = 50))
            .onSizeChanged { trackWidth.floatValue = it.width.toFloat() }
            .pointerInput(count) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downX = down.position.x
                    var isDrag = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        if (!isDrag && abs(change.position.x - downX) > viewConfiguration.touchSlop) {
                            isDrag = true
                        }
                        if (isDrag) {
                            change.consume()
                            val w = trackWidth.floatValue
                            if (w > 0f) {
                                val seg = w / count
                                // 跟手：直接写入比例，snap 动画即时生效
                                dragFrac = change.position.x.coerceIn(seg / 2f, w - seg / 2f) / w
                            }
                        }
                    }
                    val w = trackWidth.floatValue
                    if (w > 0f) {
                        val index = if (isDrag) {
                            ((dragFrac ?: 0f) * count).toInt()
                        } else {
                            // 点击：按点击横坐标定位到对应段
                            (downX / (w / count)).toInt()
                        }.coerceIn(0, count - 1)
                        // 先结束拖动（动画规格切回 tween），再切换界面：
                        // 两者在同一次快照内生效，指示块会从拖动位置平滑滑到新段中心
                        dragFrac = null
                        onSelect(items[index])
                    }
                }
            },
    ) {
        // 白色实心指示块
        if (thumbWidthPx > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset {
                        val w = trackWidth.floatValue
                        val maxLeft = (w - thumbWidthPx).coerceAtLeast(0f)
                        val left = (frac * w - thumbWidthPx / 2f).coerceIn(0f, maxLeft)
                        IntOffset(left.roundToInt(), 0)
                    }
                    .size(width = with(density) { thumbWidthPx.toDp() }, height = ThumbHeight)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(VirgoColors.ButtonFill),
            )
        }

        // 分段文字（绘制在指示块之上）
        Row(modifier = Modifier.fillMaxSize()) {
            for (panel in items) {
                val selected = panel == current
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = panel.title,
                        color = if (selected) VirgoColors.OnButton else VirgoColors.OnSurfaceVariant,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}