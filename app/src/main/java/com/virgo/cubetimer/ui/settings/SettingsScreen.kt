package com.virgo.cubetimer.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virgo.cubetimer.ui.theme.VirgoColors
import com.virgo.cubetimer.ui.timer.TimerUiState
import com.virgo.cubetimer.ui.timer.TimerViewModel

/** 设置窗格：复刻 csTimer 选项窗中的计时/打乱相关开关。 */
@Composable
fun SettingsScreen(ui: TimerUiState, vm: TimerViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SwitchRow(
            title = "15 秒观察",
            subtitle = "开始前先观察魔方，超时记 +2 / DNF",
            checked = ui.useInspection,
            onChange = { vm.setUseInspection(it) },
        )
        Spacer(modifier = Modifier.height(12.dp))
        SwitchRow(
            title = "精确到毫秒",
            subtitle = "关闭后只显示到 0.01 秒",
            checked = ui.useMilli,
            onChange = { vm.setUseMilli(it) },
        )
        Spacer(modifier = Modifier.height(12.dp))
        SwitchRow(
            title = "打乱自动换行",
            checked = ui.scrambleWrap,
            onChange = { vm.setScrambleWrap(it) },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "打乱对齐",
            color = VirgoColors.OnBackground,
            fontSize = 15.sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row {
            for ((index, label) in listOf(0 to "左", 1 to "中", 2 to "右")) {
                SegButton(
                    label = label,
                    selected = ui.scrambleAlign == index,
                    onClick = { vm.setScrambleAlign(index) },
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    subtitle: String = "",
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = VirgoColors.OnBackground, fontSize = 15.sp)
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, color = VirgoColors.OnSurfaceVariant, fontSize = 12.sp)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SegButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(56.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) VirgoColors.SurfaceVariant else VirgoColors.Background)
            .border(1.dp, VirgoColors.Border, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) VirgoColors.OnBackground else VirgoColors.OnSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}