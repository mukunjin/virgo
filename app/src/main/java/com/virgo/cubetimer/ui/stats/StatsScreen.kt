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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virgo.cubetimer.data.db.SolveEntity
import com.virgo.cubetimer.scramble.CubeState
import com.virgo.cubetimer.timer.TimeFormat
import com.virgo.cubetimer.ui.scramble.CubeNet
import com.virgo.cubetimer.ui.theme.TimerFontFamily
import com.virgo.cubetimer.ui.theme.VirgoColors
import com.virgo.cubetimer.ui.timer.TimerUiState
import com.virgo.cubetimer.ui.timer.TimerViewModel

/** 成绩窗格：分组切换 + 成绩列表（含 +2 / DNF / 删除）。 */
@Composable
fun StatsScreen(ui: TimerUiState, vm: TimerViewModel) {
    var confirmDeleteSession by remember { mutableStateOf(false) }
    /** 待确认删除的单次成绩。 */
    var confirmDeleteSolve by remember { mutableStateOf<SolveEntity?>(null) }
    /** 点击成绩后查看其对应打乱的弹窗。 */
    var detail by remember { mutableStateOf<SolveEntity?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 分组切换
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
                onClick = { vm.addSession() },
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        StatsSummary(ui, onDeleteSession = { confirmDeleteSession = true })
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
                    onDelete = { confirmDeleteSolve = solve },
                    onClick = { detail = solve },
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }

    if (confirmDeleteSession) {
        val session = ui.sessions.firstOrNull { it.id == ui.sessionId }
        AlertDialog(
            onDismissRequest = { confirmDeleteSession = false },
            title = { Text("删除分组") },
            text = { Text("将删除「${session?.name ?: ""}」及其全部成绩，且不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteSession = false
                    session?.let { vm.deleteSession(it) }
                }) { Text("删除", color = VirgoColors.TimerRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteSession = false }) { Text("取消") }
            },
        )
    }

    confirmDeleteSolve?.let { solve ->
        AlertDialog(
            onDismissRequest = { confirmDeleteSolve = null },
            title = { Text("删除成绩") },
            text = {
                Text(
                    "将删除该次成绩 " +
                        TimeFormat.prettyPenalty(solve.totalMs, solve.penalty, ui.useMilli) +
                        "，且不可恢复。",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteSolve = null
                    vm.deleteSolve(solve)
                }) { Text("删除", color = VirgoColors.TimerRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteSolve = null }) { Text("取消") }
            },
        )
    }

    detail?.let { solve ->
        val scramble = solve.scramble
        AlertDialog(
            onDismissRequest = { detail = null },
            title = { Text("本次打乱") },
            text = {
                Column {
                    Text(
                        text = scramble.ifEmpty { "（该成绩未记录打乱）" },
                        color = VirgoColors.OnBackground,
                        fontSize = 15.sp,
                        fontFamily = TimerFontFamily,
                    )
                    if (scramble.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CubeNet(
                            facelets = remember(solve.id) { CubeState.faceletsOf(scramble) },
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detail = null }) { Text("关闭") }
            },
        )
    }
}

/** 会话汇总：ao5/ao12 用大号卡片突出展示，其余指标用列表呈现。 */
@Composable
private fun StatsSummary(ui: TimerUiState, onDeleteSession: () -> Unit) {
    val s = ui.stats
    val milli = ui.useMilli
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard("ao5", TimeFormat.prettyAvgOrDnf(s.ao5, milli), Modifier.weight(1f))
            StatCard("ao12", TimeFormat.prettyAvgOrDnf(s.ao12, milli), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(VirgoColors.SurfaceVariant)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            StatRow("单次最好", s.bestSingleMs?.let { TimeFormat.pretty(it, milli) } ?: "-")
            StatRow("单次最差", s.worstSingleMs?.let { TimeFormat.pretty(it, milli) } ?: "-")
            StatRow("最好 ao5", TimeFormat.prettyAvgOrDnf(s.bestAo5, milli))
            StatRow("最好 ao12", TimeFormat.prettyAvgOrDnf(s.bestAo12, milli))
            StatRow("总平均", TimeFormat.prettyAvgOrDnf(s.meanMs, milli))
            StatRow("次数", "${s.count}")
            StatRow("DNF", "${s.dnfCount}")
            Spacer(modifier = Modifier.height(10.dp))
            // 汇总方框底部：删除当前分组（危险操作，红色胶囊 + 弹窗确认）
            DangerCapsule(label = "删除当前分组", onClick = onDeleteSession)
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(VirgoColors.SurfaceVariant)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = label.uppercase(), color = VirgoColors.OnSurfaceVariant, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = VirgoColors.OnBackground,
            fontSize = 26.sp,
            fontFamily = TimerFontFamily,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = VirgoColors.OnSurfaceVariant, fontSize = 14.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            color = VirgoColors.OnBackground,
            fontSize = 16.sp,
            fontFamily = TimerFontFamily,
        )
    }
}

@Composable
private fun SessionChip(name: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) VirgoColors.ButtonFill else VirgoColors.Surface)
            .border(1.dp, VirgoColors.Border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = name,
            color = if (selected) VirgoColors.OnButton else VirgoColors.OnSurfaceVariant,
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
    onClick: () -> Unit,
) {
    val timeText = TimeFormat.prettyPenalty(solve.totalMs, solve.penalty, useMilli)
    val timeColor = when (solve.penalty) {
        -1 -> VirgoColors.TimerRed
        2000 -> VirgoColors.TimerYellow
        else -> VirgoColors.OnBackground
    }
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(VirgoColors.SurfaceVariant)
            .clickable(onClick = onClick)
            .padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
        ActionText("×", active = false, onClick = onDelete, danger = true)
    }
}

/** 药丸形操作按钮；[danger] 为真时使用红色底以强调危险操作。 */
@Composable
private fun ActionText(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    val shape = RoundedCornerShape(percent = 50)
    val fill = when {
        danger -> VirgoColors.TimerRed
        active -> VirgoColors.ButtonFill
        else -> VirgoColors.Surface
    }
    val content = when {
        danger -> Color.White
        active -> VirgoColors.OnButton
        else -> VirgoColors.OnSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(width = 54.dp, height = 40.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, if (danger) VirgoColors.TimerRed else VirgoColors.Border, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = content, fontSize = 15.sp)
    }
}

/** 醒目的红色药丸按钮，用于危险操作（删除）。 */
@Composable
private fun DangerCapsule(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(VirgoColors.TimerRed)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}