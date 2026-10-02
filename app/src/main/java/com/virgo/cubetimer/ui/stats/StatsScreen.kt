package com.virgo.cubetimer.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virgo.cubetimer.data.db.SolveEntity
import com.virgo.cubetimer.timer.TimeFormat
import com.virgo.cubetimer.ui.theme.TimerFontFamily
import com.virgo.cubetimer.ui.theme.VirgoColors
import com.virgo.cubetimer.ui.timer.TimerUiState
import com.virgo.cubetimer.ui.timer.TimerViewModel

/** 成绩窗格：会话切换 + 成绩列表（含 +2 / DNF / 删除）。 */
@Composable
fun StatsScreen(ui: TimerUiState, vm: TimerViewModel) {
    var confirmDeleteSession by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 会话切换
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (session in ui.sessions) {
                SessionChip(
                    name = session.name,
                    selected = session.id == ui.sessionId,
                    onClick = { vm.selectSession(session.id) },
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            SessionChip(
                name = "＋ 新建",
                selected = false,
                onClick = { vm.addSession("会话 ${ui.sessions.size + 1}") },
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        StatsSummary(ui)
        Spacer(modifier = Modifier.height(8.dp))

        // 成绩列表（最新在上）
        if (ui.solves.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "暂无成绩", color = VirgoColors.Disabled, fontSize = 14.sp)
            }
        } else {
            for (solve in ui.solves.asReversed()) {
                SolveRow(
                    index = ui.solves.indexOf(solve) + 1,
                    solve = solve,
                    useMilli = ui.useMilli,
                    onTogglePlus2 = {
                        vm.setPenalty(solve, if (solve.penalty == 2000) 0 else 2000)
                    },
                    onToggleDnf = {
                        vm.setPenalty(solve, if (solve.penalty == -1) 0 else -1)
                    },
                    onDelete = { vm.deleteSolve(solve) },
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        TextButton(onClick = { confirmDeleteSession = true }) {
            Text(text = "删除当前会话", color = VirgoColors.OnSurfaceVariant, fontSize = 13.sp)
        }
    }

    if (confirmDeleteSession) {
        val session = ui.sessions.firstOrNull { it.id == ui.sessionId }
        AlertDialog(
            onDismissRequest = { confirmDeleteSession = false },
            title = { Text("删除会话") },
            text = { Text("将删除「${session?.name ?: ""}」及其全部成绩，且不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteSession = false
                    session?.let { vm.deleteSession(it) }
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteSession = false }) { Text("取消") }
            },
        )
    }
}

/** 会话汇总：次数、单次最好/最差、平均、总平均、最好 ao5/ao12。 */
@Composable
private fun StatsSummary(ui: TimerUiState) {
    val s = ui.stats
    val milli = ui.useMilli
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryItem("次数", "${s.count}", Modifier.weight(1f))
            SummaryItem("DNF", "${s.dnfCount}", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryItem(
                "单次最好",
                s.bestSingleMs?.let { TimeFormat.pretty(it, milli) } ?: "-",
                Modifier.weight(1f),
            )
            SummaryItem(
                "单次最差",
                s.worstSingleMs?.let { TimeFormat.pretty(it, milli) } ?: "-",
                Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryItem("ao5", TimeFormat.prettyAvgOrDnf(s.ao5, milli), Modifier.weight(1f))
            SummaryItem("ao12", TimeFormat.prettyAvgOrDnf(s.ao12, milli), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryItem("最好 ao5", TimeFormat.prettyAvgOrDnf(s.bestAo5, milli), Modifier.weight(1f))
            SummaryItem("最好 ao12", TimeFormat.prettyAvgOrDnf(s.bestAo12, milli), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryItem("总平均", TimeFormat.prettyAvgOrDnf(s.meanMs, milli), Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Text(text = "$label ", color = VirgoColors.OnSurfaceVariant, fontSize = 13.sp)
        Text(
            text = value,
            color = VirgoColors.OnBackground,
            fontSize = 13.sp,
            fontFamily = TimerFontFamily,
        )
    }
}

@Composable
private fun SessionChip(name: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) VirgoColors.SurfaceVariant else VirgoColors.Background)
            .border(1.dp, VirgoColors.Border, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = name,
            color = if (selected) VirgoColors.OnBackground else VirgoColors.OnSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun SolveRow(
    index: Int,
    solve: SolveEntity,
    useMilli: Boolean,
    onTogglePlus2: () -> Unit,
    onToggleDnf: () -> Unit,
    onDelete: () -> Unit,
) {
    val timeText = TimeFormat.prettyPenalty(solve.totalMs, solve.penalty, useMilli)
    val timeColor = when (solve.penalty) {
        -1 -> VirgoColors.TimerRed
        2000 -> VirgoColors.TimerYellow
        else -> VirgoColors.OnBackground
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "$index",
            color = VirgoColors.Disabled,
            fontSize = 12.sp,
            modifier = Modifier.width(28.dp),
        )
        Text(
            text = timeText,
            color = timeColor,
            fontSize = 18.sp,
            fontFamily = TimerFontFamily,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        ActionText("+2", solve.penalty == 2000, onTogglePlus2)
        ActionText("DNF", solve.penalty == -1, onToggleDnf)
        ActionText("×", false, onDelete)
    }
}

@Composable
private fun ActionText(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = if (label == "+2") 34.dp else 34.dp, height = 28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) VirgoColors.SurfaceVariant else VirgoColors.Background)
            .border(1.dp, VirgoColors.Border, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (active) VirgoColors.OnBackground else VirgoColors.OnSurfaceVariant,
            fontSize = 12.sp,
        )
    }
}