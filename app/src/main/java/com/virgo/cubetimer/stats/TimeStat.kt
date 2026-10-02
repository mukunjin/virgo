package com.virgo.cubetimer.stats

import com.virgo.cubetimer.data.db.SolveEntity
import kotlin.math.ceil

/**
 * 统计计算，复刻 csTimer `stats/timestat.js` + `stats/stats.js` 的语义。
 *
 * 关键规则：
 * - 单次成绩取值 = `罚时 + 用时`（DNF 记为 -1），并向下取整到 10ms（`timeAtDim`）。
 * - 修剪规则来自属性 `trim`（默认 `p5`）与 `trimr`（默认 `a`，即沿用 `trim`）：
 *   `getNTrim(n, 'p5') = ceil(n * 5 / 100)`。
 * - 一次平均中 DNF 数 > 右侧修剪数时，该平均记为 DNF。
 */
object TimeStat {

    /** csTimer 中 DNF 的取值。 */
    const val DNF: Long = -1L

    /** 默认展示的两次平均长度（csTimer `stat1/stat2`）。 */
    val DEFAULT_AVG_SIZES = listOf(5, 12)

    private val dnfComparator = Comparator<Long> { a, b ->
        when {
            a == b -> 0
            a == DNF -> 1
            b == DNF -> -1
            else -> a.compareTo(b)
        }
    }

    /** `stats.js` 的 `timeAtDim(0, idx)`：加罚时后按显示精度向下取整。 */
    fun valueOf(solve: SolveEntity, useMilli: Boolean = true): Long {
        if (solve.penalty == -1) return DNF
        val raw = solve.totalMs + if (solve.penalty > 0) solve.penalty.toLong() else 0L
        val roundMilli = if (useMilli) 1L else 10L
        return roundMilli * (raw / roundMilli)
    }

    /** `timestat.js` 的 `getNTrim`。 */
    fun getNTrim(n: Int, ntrim: String): Int = when {
        ntrim.startsWith("p") -> ceil(n / 100.0 * ntrim.substring(1).toInt()).toInt()
        ntrim == "m" -> maxOf(0, n shr 1)
        else -> ntrim.toIntOrNull() ?: 0
    }

    /** `timestat.js` 的 `getNTrimLR`：返回 `[左修剪数, 右修剪数]`。 */
    fun trimLR(size: Int, trim: String, trimRight: String): Pair<Int, Int> {
        val left = getNTrim(size, trim)
        val right = getNTrim(size, if (trimRight == "a") trim else trimRight)
        return if (left + right == size) {
            Pair(maxOf(left - 1, 0), maxOf(right - 1, 0))
        } else {
            Pair(left, right)
        }
    }

    /**
     * 计算 [window]（长度即平均长度）的平均值；返回 `null` 表示 DNF。
     */
    fun average(window: List<Long>, trim: String, trimRight: String): Double? {
        val size = window.size
        if (size == 0) return null
        val (tl, tr) = trimLR(size, trim, trimRight)
        if (size - tl - tr <= 0) return null
        // 平均中 DNF 数超过右侧修剪数时为 DNF
        if (window.count { it == DNF } > tr) return null
        val sorted = window.sortedWith(dnfComparator)
        val kept = sorted.subList(tl, size - tr)
        return kept.sum().toDouble() / kept.size
    }

    /** 会话平均（csTimer `getMean`）：忽略 DNF；全为 DNF 时返回 `null`。 */
    fun mean(values: List<Long>): Double? {
        val valid = values.filter { it != DNF }
        if (valid.isEmpty()) return null
        return valid.sum().toDouble() / valid.size
    }

    /** 单次最好（忽略 DNF）。 */
    fun bestSingle(values: List<Long>): Long? = values.filter { it != DNF }.minOrNull()

    /** 单次最差（忽略 DNF，与 csTimer 的 `worstTime` 一致）。 */
    fun worstSingle(values: List<Long>): Long? = values.filter { it != DNF }.maxOrNull()

    /** 最近 [size] 次的平均（不足 [size] 次返回 `null`）。 */
    fun lastAverage(values: List<Long>, size: Int, trim: String, trimRight: String): Double? {
        if (values.size < size) return null
        return average(values.subList(values.size - size, values.size), trim, trimRight)
    }

    /** 全程最好的一次平均（含所有长度为 [size] 的窗口）。 */
    fun bestAverage(values: List<Long>, size: Int, trim: String, trimRight: String): Double? {
        if (values.size < size) return null
        var best: Double? = null
        for (i in 0..values.size - size) {
            val avg = average(values.subList(i, i + size), trim, trimRight) ?: continue
            if (best == null || avg < best) best = avg
        }
        return best
    }
}

/** 一个会话的汇总统计。 */
data class SessionStats(
    val count: Int = 0,
    val dnfCount: Int = 0,
    val bestSingleMs: Long? = null,
    val worstSingleMs: Long? = null,
    val meanMs: Double? = null,
    val ao5: Double? = null,
    val ao12: Double? = null,
    val bestAo5: Double? = null,
    val bestAo12: Double? = null,
) {
    companion object {
        fun of(
            solves: List<SolveEntity>,
            trim: String,
            trimRight: String,
            useMilli: Boolean = true,
        ): SessionStats {
            val values = solves.map { TimeStat.valueOf(it, useMilli) }
            return SessionStats(
                count = values.size,
                dnfCount = values.count { it == TimeStat.DNF },
                bestSingleMs = TimeStat.bestSingle(values),
                worstSingleMs = TimeStat.worstSingle(values),
                meanMs = TimeStat.mean(values),
                ao5 = TimeStat.lastAverage(values, 5, trim, trimRight),
                ao12 = TimeStat.lastAverage(values, 12, trim, trimRight),
                bestAo5 = TimeStat.bestAverage(values, 5, trim, trimRight),
                bestAo12 = TimeStat.bestAverage(values, 12, trim, trimRight),
            )
        }
    }
}