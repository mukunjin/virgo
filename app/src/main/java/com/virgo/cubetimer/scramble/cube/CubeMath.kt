package com.virgo.cubetimer.scramble.cube

/**
 * 移植自 csTimer mathlib.js 的排列辅助函数（仅 333 打乱所需子集）。
 *
 * 关键语义：JS 中所有位运算按 32 位有符号处理，`>>` 为算术右移、
 * `>>>` 为逻辑右移；本移植用 Kotlin `Int` + `shr`/`ushr` 严格对齐。
 */
object CubeMath {

    private const val VALL = 0x76543210
    private val VALH = 0xfedcba98.toInt() // -19088744

    /**
     * mathlib.getNPerm(arr, n, even)。
     *
     * JS: `return even < 0 ? (idx >> 1) : idx;`
     * 仅移植 n < 16 分支（333 只用 n = 12 / 8）。
     */
    fun getNPerm(arr: IntArray, n: Int, even: Int = 0): Int {
        var idx = 0
        var vall = VALL
        var valh = VALH
        for (i in 0 until n - 1) {
            val v = arr[i] shl 2
            idx *= n - i
            if (v >= 32) {
                idx += (valh shr (v - 32)) and 0xf
                valh -= 0x11111110 shl (v - 32)
            } else {
                idx += (vall shr v) and 0xf
                valh -= 0x11111111
                vall -= 0x11111110 shl v
            }
        }
        return if (even < 0) idx shr 1 else idx
    }

    /** mathlib.getNParity(idx, n)。 */
    fun getNParity(idx0: Int, n: Int): Int {
        var idx = idx0
        var p = 0
        for (i in n - 2 downTo 0) {
            p = p xor (idx % (n - i))
            idx /= n - i
        }
        return p and 1
    }
}