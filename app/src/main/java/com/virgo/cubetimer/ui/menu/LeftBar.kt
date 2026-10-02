package com.virgo.cubetimer.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virgo.cubetimer.ui.theme.VirgoColors
import com.virgo.cubetimer.ui.timer.LeftBarWidth

/** 左侧图标列可打开的浮动窗格。 */
enum class Panel(val title: String) {
    NONE(""),
    SETTINGS("设置"),
    STATS("成绩"),
    ABOUT("关于"),
}

/**
 * 左侧竖向图标列，复刻 csTimer 的 `#leftbar`
 * （顶部 Logo + 一列方形按钮；选中态用灰底表示）。
 */
@Composable
fun LeftBar(current: Panel, onSelect: (Panel) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(LeftBarWidth)
            .padding(top = 8.dp, start = 4.dp, end = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Virgo",
            color = VirgoColors.OnBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 6.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        for (panel in listOf(Panel.SETTINGS, Panel.STATS, Panel.ABOUT)) {
            BarButton(
                label = panel.title,
                selected = current == panel,
                onClick = { onSelect(panel) },
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun BarButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) VirgoColors.SurfaceVariant else VirgoColors.Background)
            .border(1.dp, VirgoColors.Border, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) VirgoColors.OnBackground else VirgoColors.OnSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

/**
 * 浮动窗格，复刻 csTimer `.mywindow`：带边框的浮层 + 标题栏 + 关闭按钮。
 */
@Composable
fun FloatingPanel(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(VirgoColors.Background)
            .border(1.dp, VirgoColors.Border, RoundedCornerShape(6.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VirgoColors.Surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = VirgoColors.OnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "×", color = VirgoColors.OnSurfaceVariant, fontSize = 18.sp)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            content = content,
        )
    }
}