package com.virgo.cubetimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.virgo.cubetimer.ui.about.AboutScreen
import com.virgo.cubetimer.ui.menu.FloatingPanel
import com.virgo.cubetimer.ui.menu.LeftBar
import com.virgo.cubetimer.ui.menu.Panel
import com.virgo.cubetimer.ui.settings.SettingsScreen
import com.virgo.cubetimer.ui.stats.StatsScreen
import com.virgo.cubetimer.ui.theme.VirgoColors
import com.virgo.cubetimer.ui.theme.VirgoTheme
import com.virgo.cubetimer.ui.timer.LeftBarWidth
import com.virgo.cubetimer.ui.timer.TimerScreen
import com.virgo.cubetimer.ui.timer.TimerViewModel

/**
 * 原生界面根节点：左侧图标列 + 计时主界面 + 浮动窗格（复刻 csTimer 布局）。
 */
@Composable
fun VirgoApp() {
    VirgoTheme {
        val vm: TimerViewModel = viewModel()
        val ui by vm.ui.collectAsState()
        var panel by remember { mutableStateOf(Panel.NONE) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VirgoColors.Background),
        ) {
            TimerScreen(ui = ui, vm = vm, modifier = Modifier.fillMaxSize())

            LeftBar(
                current = panel,
                onSelect = { panel = if (panel == it) Panel.NONE else it },
                modifier = Modifier.align(Alignment.TopStart),
            )

            if (panel != Panel.NONE) {
                FloatingPanel(
                    title = panel.title,
                    onClose = { panel = Panel.NONE },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = LeftBarWidth + 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
                        .fillMaxWidth()
                        .heightIn(max = 560.dp),
                ) {
                    when (panel) {
                        Panel.SETTINGS -> SettingsScreen(ui = ui, vm = vm)
                        Panel.STATS -> StatsScreen(ui = ui, vm = vm)
                        Panel.ABOUT -> AboutScreen()
                        Panel.NONE -> Unit
                    }
                }
            }
        }
    }
}