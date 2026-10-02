package com.virgo.cubetimer.timer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 计时配置，对齐 csTimer `timer.js` 的属性默认值。
 */
data class TimerConfig(
    /** 阶段数（csTimer `phases`，默认 1）。 */
    val phases: Int = 1,
    /** 是否开启 15 秒观察（csTimer `useIns`，默认关闭）。 */
    val useInspection: Boolean = false,
    /** 长按进入就绪的时长（csTimer `preTime`，默认 300ms）。 */
    val preTimeMs: Long = 300L,
)

/** 一次完整计时的结果。 */
data class TimerResult(
    /** 总用时（毫秒）。 */
    val totalMs: Long,
    /** 罚时：`0` 正常、`2000` 表示 +2、`-1` 表示 DNF。 */
    val penalty: Int,
    /** 各阶段用时（毫秒，按阶段顺序）。 */
    val splitsMs: List<Long>,
)

/**
 * 计时状态机，逐条复刻 csTimer `timer.js` 的键盘/触摸计时逻辑
 * （触摸等同于「按住空格」：按下 = keydown，全部松手 = keyup）。
 *
 * 默认关闭观察时的状态迁移：
 * ```
 * IDLE --按下--> (300ms 长按) READY --松手--> RUNNING --按下--> STOPPED --松手--> IDLE
 * ```
 * 开启观察时：`IDLE --按下--> READY_INSPECT --松手--> INSPECTING --按下/长按--> READY --松手--> RUNNING`。
 */
class TimerEngine(
    private val scope: CoroutineScope,
    private val config: () -> TimerConfig,
    private val onStopped: (TimerResult) -> Unit,
    private val onChanged: () -> Unit,
) {

    enum class Status { IDLE, READY, INSPECTING, READY_INSPECT, STOPPED, RUNNING }

    var status: Status = Status.IDLE
        private set

    private var totalPhases = 1
    private var startMs = 0L
    private var splits = LongArray(2)
    private var penalty = 0
    private var lastDownMs = 0L
    private var lastStopMs = 0L
    private var pressReadyJob: Job? = null

    /** 是否处于「已清空」显示态（对应 timer.js 的 isCleared）。 */
    private var cleared = true
    private var lastResultMs = 0L

    private fun now(): Long = System.nanoTime() / 1_000_000

    /** 触摸/按键按下。 */
    fun press() {
        val t = now()
        if (t - lastDownMs < 200) return
        when (status) {
            Status.RUNNING -> {
                lastDownMs = t
                splits[totalPhases] = t - startMs
                status = Status.STOPPED
                lastStopMs = t
                cleared = false
                lastResultMs = splits[totalPhases]
                onStopped(
                    TimerResult(
                        totalMs = splits[totalPhases],
                        penalty = penalty,
                        splitsMs = (1..totalPhases).map { i ->
                            splits[i] - if (i > 1) splits[i - 1] else 0L
                        },
                    )
                )
            }

            Status.IDLE, Status.INSPECTING -> {
                val readyFrom = if (config().useInspection) Status.INSPECTING else Status.IDLE
                if (status == readyFrom && pressReadyJob == null) {
                    pressReadyJob = scope.launch {
                        delay(config().preTimeMs)
                        pressReady()
                    }
                } else if (status == Status.IDLE && config().useInspection) {
                    startMs = t
                    status = Status.READY_INSPECT
                }
            }

            else -> Unit
        }
        onChanged()
    }

    /** 触摸/按键松手。 */
    fun release() {
        val t = now()
        when (status) {
            Status.STOPPED -> status = Status.IDLE

            Status.IDLE, Status.INSPECTING -> {
                clearPressReady()
                if (t - lastStopMs < 500) {
                    onChanged()
                    return
                }
            }

            Status.READY -> {
                lastDownMs = t
                val insTime = if (config().useInspection) t - startMs else 0L
                startMs = t
                penalty = when {
                    insTime > 17_000 -> -1
                    insTime > 15_000 -> 2000
                    else -> 0
                }
                totalPhases = config().phases.coerceAtLeast(1)
                splits = LongArray(totalPhases + 1)
                cleared = false
                status = Status.RUNNING
            }

            Status.READY_INSPECT -> {
                startMs = t
                status = Status.INSPECTING
            }

            else -> Unit
        }
        onChanged()
    }

    /** 取消当前计时并回到空闲态（对应 ESC / 切换会话等重置场景）。 */
    fun cancel() {
        clearPressReady()
        status = Status.IDLE
        cleared = true
        lastResultMs = 0L
        onChanged()
    }

    private fun pressReady() {
        if (status == Status.IDLE || status == Status.INSPECTING) {
            if (status == Status.IDLE) {
                cleared = true
                lastResultMs = 0L
            }
            status = Status.READY
            pressReadyJob = null
            onChanged()
        }
    }

    private fun clearPressReady() {
        pressReadyJob?.cancel()
        pressReadyJob = null
    }

    /** 当前已运行/观察的毫秒数。 */
    fun elapsedMs(): Long = when (status) {
        Status.RUNNING, Status.INSPECTING, Status.READY_INSPECT -> now() - startMs
        else -> 0L
    }

    /** 非运行态下应静态显示的毫秒数。 */
    fun staticDisplayMs(): Long = when (status) {
        Status.STOPPED -> splits[totalPhases]
        Status.READY -> 0L
        Status.IDLE -> if (cleared) 0L else lastResultMs
        else -> 0L
    }

    val isRunning: Boolean get() = status == Status.RUNNING
    val isInspecting: Boolean get() = status == Status.INSPECTING || status == Status.READY_INSPECT
}