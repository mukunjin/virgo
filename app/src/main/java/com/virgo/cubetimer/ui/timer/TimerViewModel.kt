package com.virgo.cubetimer.ui.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.virgo.cubetimer.data.db.SessionEntity
import com.virgo.cubetimer.data.db.SolveEntity
import com.virgo.cubetimer.data.db.VirgoDatabase
import com.virgo.cubetimer.data.repo.SessionRepository
import com.virgo.cubetimer.data.repo.SettingsRepository
import com.virgo.cubetimer.scramble.CubeState
import com.virgo.cubetimer.scramble.Scramble333
import com.virgo.cubetimer.stats.SessionStats
import com.virgo.cubetimer.timer.TimeFormat
import com.virgo.cubetimer.timer.TimerConfig
import com.virgo.cubetimer.timer.TimerEngine
import com.virgo.cubetimer.timer.TimerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 计时页的界面状态。 */
data class TimerUiState(
    val status: TimerEngine.Status = TimerEngine.Status.IDLE,
    val scramble: String = "",
    /** 当前打乱的贴纸状态（54 字符 URFDLB），空串表示尚未生成。 */
    val netFacelets: String = "",
    val generating: Boolean = true,
    /** 打乱历史中是否存在更早的一条。 */
    val canPrev: Boolean = false,
    val solves: List<SolveEntity> = emptyList(),
    val stats: SessionStats = SessionStats(),
    val sessions: List<SessionEntity> = emptyList(),
    val sessionId: Long = 0L,
    val sessionName: String = "",
    val useInspection: Boolean = false,
    val useMilli: Boolean = true,
    val scrambleAlign: Int = 1,
)

/**
 * 计时页 ViewModel：持有 [TimerEngine]、当前会话与成绩列表，并负责生成 333 打乱。
 */
class TimerViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsRepository(app)
    private val repo = SessionRepository(VirgoDatabase.get(app).dao())

    private val _state = MutableStateFlow(TimerUiState())
    val ui: StateFlow<TimerUiState> = _state.asStateFlow()

    /** 重量级求解表只在首次使用时构建，且始终在后台线程访问。 */
    private val scrambler: Scramble333 by lazy { Scramble333.create() }

    private var scrambleJob: Job? = null
    private var solveJob: Job? = null

    /** 打乱历史与当前位置（对应 csTimer 打乱窗的「上一条 / 下一条」）。 */
    private val scrambleHistory = mutableListOf<String>()
    private var scrambleIndex = -1

    private val engine = TimerEngine(
        scope = viewModelScope,
        config = {
            TimerConfig(
                phases = 1,
                useInspection = settings.useInspection,
                preTimeMs = 300L,
            )
        },
        onStopped = { saveSolve(it) },
        onChanged = { publishStatus() },
    )

    init {
        viewModelScope.launch {
            val defaultId = repo.ensureDefaultSession()
            val saved = settings.currentSessionId
            val list = repo.observeSessions().first()
            // 避免使用已被删除的会话 id。
            val target = if (list.any { it.id == saved }) saved else defaultId
            attachSession(target)
            _state.update { it.copy(sessions = list) }
            generateScramble()
        }
        viewModelScope.launch {
            repo.observeSessions().collect { list ->
                _state.update { st ->
                    st.copy(
                        sessions = list,
                        sessionName = list.firstOrNull { it.id == st.sessionId }?.name ?: st.sessionName,
                    )
                }
            }
        }
        syncSettings()
    }

    // ---- 计时输入 ----

    fun onPress() = engine.press()

    fun onRelease() = engine.release()

    /** 运行中/观察中的实时毫秒数（供每帧刷新）。 */
    fun elapsedMs(): Long = engine.elapsedMs()

    val isRunning: Boolean get() = engine.isRunning

    val isInspecting: Boolean get() = engine.isInspecting

    /** 当前 LCD 应显示的文本。 */
    fun displayText(): String {
        val useMilli = _state.value.useMilli
        return when (_state.value.status) {
            TimerEngine.Status.RUNNING -> TimeFormat.pretty(engine.elapsedMs(), useMilli)
            TimerEngine.Status.INSPECTING -> inspectionText(engine.elapsedMs())
            // 就绪（正式计时开始前的长按）仍显示观察倒计时，而不是 0.000
            TimerEngine.Status.READY ->
                if (_state.value.useInspection) inspectionText(engine.elapsedMs())
                else TimeFormat.pretty(0L, useMilli)
            // 刚拍表：显示本次成绩
            TimerEngine.Status.STOPPED -> TimeFormat.pretty(engine.staticDisplayMs(), useMilli)
            // 空闲：自动读取当前分组最新一条成绩；无成绩则显示 0.000
            TimerEngine.Status.IDLE -> latestSolveText(useMilli)
        }
    }

    /** 空闲态显示当前分组最新成绩；无成绩时显示 0.000。 */
    private fun latestSolveText(useMilli: Boolean): String {
        val latest = _state.value.solves.lastOrNull() ?: return TimeFormat.pretty(0L, useMilli)
        return TimeFormat.prettyPenalty(latest.totalMs, latest.penalty, useMilli)
    }

    /** 观察倒计时文本：15~1 秒、超时 +2、再超时 DNF。 */
    private fun inspectionText(elapsedMs: Long): String = when {
        elapsedMs > 17_000 -> "DNF"
        elapsedMs > 15_000 -> "+2"
        else -> (15 - (elapsedMs / 1000).toInt()).toString()
    }

    // ---- 打乱 ----

    /** 生成新打乱并追加到历史末尾。 */
    fun generateScramble() {
        if (scrambleJob?.isActive == true) return
        _state.update { it.copy(generating = true) }
        scrambleJob = viewModelScope.launch {
            val text = withContext(Dispatchers.Default) { scrambler.getRandomScramble() }
            // 生成新打乱时丢弃当前之后的历史分支
            if (scrambleIndex < scrambleHistory.size - 1) {
                scrambleHistory.subList(scrambleIndex + 1, scrambleHistory.size).clear()
            }
            scrambleHistory.add(text)
            scrambleIndex = scrambleHistory.size - 1
            applyCurrentScramble()
        }
    }

    /** 上一条打乱。 */
    fun prevScramble() {
        if (scrambleIndex <= 0) return
        scrambleIndex--
        applyCurrentScramble()
    }

    /** 下一条打乱；已在最新一条时生成新的。 */
    fun nextScramble() {
        if (scrambleIndex < scrambleHistory.size - 1) {
            scrambleIndex++
            applyCurrentScramble()
        } else {
            generateScramble()
        }
    }

    private fun applyCurrentScramble() {
        val text = scrambleHistory.getOrNull(scrambleIndex).orEmpty()
        _state.update {
            it.copy(
                scramble = text,
                netFacelets = if (text.isEmpty()) "" else CubeState.faceletsOf(text),
                generating = false,
                canPrev = scrambleIndex > 0,
            )
        }
    }

    // ---- 分组 ----

    fun selectSession(id: Long) {
        if (id == _state.value.sessionId) return
        settings.currentSessionId = id
        attachSession(id)
        engine.cancel()
        generateScramble()
    }

    /** 建议的新分组名：取现有名字中最大编号 + 1，避免删除后重名。 */
    fun suggestGroupName(): String {
        val maxIndex = _state.value.sessions
            .mapNotNull { it.name.substringAfterLast(' ').toIntOrNull() }
            .maxOrNull() ?: 0
        return "分组 ${maxIndex + 1}"
    }

    /** 新建分组（使用用户输入的名字）。 */
    fun addSession(name: String) {
        viewModelScope.launch {
            val id = repo.addSession(name)
            selectSession(id)
        }
    }

    fun renameSession(id: Long, name: String) {
        viewModelScope.launch { repo.renameSession(id, name) }
    }

    fun deleteSession(session: SessionEntity) {
        viewModelScope.launch {
            repo.deleteSession(session)
            val remaining = _state.value.sessions.filter { it.id != session.id }
            val next = remaining.firstOrNull()?.id ?: repo.ensureDefaultSession()
            selectSession(next)
        }
    }

    // ---- 成绩 ----

    fun setPenalty(solve: SolveEntity, penalty: Int) {
        viewModelScope.launch { repo.setPenalty(solve, penalty) }
    }

    fun deleteSolve(solve: SolveEntity) {
        viewModelScope.launch { repo.deleteSolve(solve) }
    }

    private fun saveSolve(result: TimerResult) {
        val scramble = _state.value.scramble
        val sessionId = _state.value.sessionId
        if (sessionId == 0L) return
        viewModelScope.launch {
            repo.appendSolve(
                sessionId = sessionId,
                penalty = result.penalty,
                totalMs = result.totalMs,
                scramble = scramble,
                splitsCsv = if (result.splitsMs.size > 1) result.splitsMs.joinToString(",") else "",
            )
            generateScramble()
        }
    }

    // ---- 设置 ----

    fun setUseInspection(value: Boolean) {
        settings.useInspection = value
        _state.update { it.copy(useInspection = value) }
        engine.cancel()
    }

    fun setScrambleAlign(value: Int) {
        settings.scrambleAlign = value
        _state.update { it.copy(scrambleAlign = value) }
    }

    /** 切换「精确到毫秒」，并重算统计（统计值会随显示精度取整）。 */
    fun setUseMilli(value: Boolean) {
        settings.useMilli = value
        val solves = _state.value.solves
        _state.update {
            it.copy(
                useMilli = value,
                stats = SessionStats.of(solves, settings.trim, settings.trimRight, value),
            )
        }
    }

    // ---- 内部 ----

    private fun attachSession(id: Long) {
        _state.update { it.copy(sessionId = id, solves = emptyList(), stats = SessionStats()) }
        solveJob?.cancel()
        solveJob = viewModelScope.launch {
            repo.observeSolves(id).collect { list ->
                val stats = SessionStats.of(list, settings.trim, settings.trimRight, settings.useMilli)
                _state.update {
                    it.copy(
                        solves = list,
                        stats = stats,
                        sessionName = it.sessions.firstOrNull { s -> s.id == id }?.name ?: "",
                    )
                }
            }
        }
    }

    private fun syncSettings() {
        _state.update {
            it.copy(
                useInspection = settings.useInspection,
                useMilli = settings.useMilli,
                scrambleAlign = settings.scrambleAlign,
            )
        }
    }

    private fun publishStatus() {
        _state.update { it.copy(status = engine.status) }
    }
}