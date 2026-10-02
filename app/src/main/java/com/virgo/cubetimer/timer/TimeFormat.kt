package com.virgo.cubetimer.timer

/**
 * 时间格式化，逐条复刻 csTimer `kernel.js` 的 `pretty()`。
 * `timeFormat = 'h'`（`H:MM:SS.XX(X)`，无小时段则省略）。
 * Virgo 默认开启毫秒（`useMilli = true`），即精确到小数点后三位。
 *
 * 示例：`12345` -> `"12.345"`；`65430` -> `"1:05.430"`。
 */
object TimeFormat {

    fun pretty(timeMs: Long, useMilli: Boolean = true): String {
        if (timeMs < 0) return "DNF"
        val scale = if (useMilli) 1L else 10L
        val unit = if (useMilli) 1000L else 100L
        var time = Math.floorDiv(timeMs, scale)
        val bits = (time % unit).toInt()
        time = Math.floorDiv(time, unit)
        val secs = time % 60
        val mins = (time / 60) % 60
        val hours = time / 3600

        val sb = StringBuilder()
        if (hours > 0) {
            sb.append(hours).append(':')
            if (mins < 10) sb.append('0')
            sb.append(mins).append(':')
            if (secs < 10) sb.append('0')
        } else if (mins > 0) {
            sb.append(mins).append(':')
            if (secs < 10) sb.append('0')
        }
        sb.append(secs).append('.')
        if (bits < 10) sb.append('0')
        if (useMilli && bits < 100) sb.append('0')
        sb.append(bits)
        return sb.toString()
    }

    /** csTimer `kpround`：先把毫秒四舍五入到显示精度，再格式化。 */
    fun prettyRounded(valueMs: Double, useMilli: Boolean = true): String {
        val mul = if (useMilli) 1.0 else 10.0
        val rounded = (Math.round(valueMs / mul) * mul).toLong()
        return pretty(rounded, useMilli)
    }

    /** csTimer `pround`：同上，但针对可空的平均值（`null` 表示 DNF）。 */
    fun prettyAvgOrDnf(valueMs: Double?, useMilli: Boolean = true): String =
        if (valueMs == null) "DNF" else prettyRounded(valueMs, useMilli)

    /** csTimer `pretty([penalty, total])`：`-1` -> DNF，`2000` -> 附加 "+"。 */
    fun prettyPenalty(totalMs: Long, penalty: Int, useMilli: Boolean = true): String {
        if (penalty == -1) return "DNF"
        val base = pretty(totalMs, useMilli)
        return if (penalty == 2000) "$base+" else base
    }
}