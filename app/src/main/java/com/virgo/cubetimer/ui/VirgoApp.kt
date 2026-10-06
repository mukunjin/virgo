package com.virgo.cubetimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.virgo.cubetimer.ui.about.AboutScreen
import com.virgo.cubetimer.ui.menu.CapsuleBar
import com.virgo.cubetimer.ui.menu.Panel
import com.virgo.cubetimer.ui.settings.SettingsScreen
import com.virgo.cubetimer.ui.stats.SolveDetailOverlay
import com.virgo.cubetimer.ui.stats.StatsScreen
import com.virgo.cubetimer.ui.theme.VirgoColors
import com.virgo.cubetimer.ui.theme.VirgoTheme
import com.virgo.cubetimer.ui.timer.TimerScreen
import com.virgo.cubetimer.ui.timer.TimerViewModel
import com.virgo.cubetimer.ui.timer.hidesOtherUi

/**
 * 原生界面根节点：整屏在「计时 / 成绩 / 设置 / 关于 Virgo」之间切换，
 * 底部中央常驻分段胶囊作为切换入口。
 */
@Composable
fun VirgoApp() {
    VirgoTheme {
        val vm: TimerViewModel = viewModel()
        val ui by vm.ui.collectAsState()
        var panel by remember { mutableStateOf(Panel.TIMER) }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(VirgoColors.Background),
        ) {
            when (panel) {
                Panel.TIMER -> TimerScreen(ui = ui, vm = vm, modifier = Modifier.fillMaxSize())
                Panel.STATS -> SheetScreen(title = "成绩") { StatsScreen(ui = ui, vm = vm) }
                Panel.SETTINGS -> SheetScreen(title = "设置") { SettingsScreen(ui = ui, vm = vm) }
                Panel.ABOUT -> SheetScreen(title = "关于 Virgo") { AboutScreen() }
            }

            // 观察开始到拍表之间隐去底部胶囊，屏幕上只留时间
            if (!ui.status.hidesOtherUi) {
                CapsuleBar(
                    current = panel,
                    onSelect = { panel = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .width(minOf(maxWidth * 0.76f, 360.dp)),
                )
            }

            // 成绩详情：应用内全屏浮层（避免独立弹窗窗口带来的延迟）
            ui.detailSolve?.let { solve ->
                SolveDetailOverlay(
                    solve = solve,
                    useMilli = ui.useMilli,
                    onClose = { vm.closeSolveDetail() },
                )
            }
        }
    }
}

/** 非计时界面的通用容器：顶部标题 + 可滚动内容 + 底部为胶囊留白。 */
@Composable
private fun SheetScreen(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 120.dp),
    ) {
        Text(
            text = title,
            color = VirgoColors.OnBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(14.dp))
        content()
    }
}