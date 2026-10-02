package com.virgo.cubetimer.scramble.rng

import kotlin.math.abs

/**
 * 移植自 csTimer isaac.js 的 ISAAC PRNG。
 *
 * 严格对齐 JS 的 32 位回绕与位移语义：
 * - `add()` 为 32 位无符号加法（Kotlin `Int` 溢出天然回绕）；
 * - `<<` -> `shl`，`>>>` -> `ushr`，`>>` -> `shr`；
 * - `random()` 与 JS 完全一致的两次抽值组合。
 */
class IsaacRandom : RandomSource {

    private val m = IntArray(256)
    private val r = IntArray(256)
    private var acc = 0
    private var brs = 0
    private var cnt = 0
    private var gnt = 0

    fun reset() {
        acc = 0
        brs = 0
        cnt = 0
        gnt = 0
        m.fill(0)
        r.fill(0)
    }

    /** isaac.js 的 32 位安全加法。 */
    private fun add(x: Int, y: Int): Int {
        val lsb = (x and 0xffff) + (y and 0xffff)
        val msb = (x ushr 16) + (y ushr 16) + (lsb ushr 16)
        return (msb shl 16) or (lsb and 0xffff)
    }

    /** isaac.js seed(s)：s 为种子数组（每个元素按 32 位参与运算）。 */
    fun seed(s: IntArray) {
        var a = GOLDEN
        var b = GOLDEN
        var c = GOLDEN
        var d = GOLDEN
        var e = GOLDEN
        var f = GOLDEN
        var g = GOLDEN
        var h = GOLDEN

        reset()
        for (i in s.indices) {
            r[i and 0xff] = r[i and 0xff] + s[i]
        }

        fun seedMix() {
            a = a xor (b shl 11); d = add(d, a); b = add(b, c)
            b = b xor (c ushr 2); e = add(e, b); c = add(c, d)
            c = c xor (d shl 8); f = add(f, c); d = add(d, e)
            d = d xor (e ushr 16); g = add(g, d); e = add(e, f)
            e = e xor (f shl 10); h = add(h, e); f = add(f, g)
            f = f xor (g ushr 4); a = add(a, f); g = add(g, h)
            g = g xor (h shl 8); b = add(b, g); h = add(h, a)
            h = h xor (a ushr 9); c = add(c, h); a = add(a, b)
        }

        repeat(4) { seedMix() }

        var i = 0
        while (i < 256) {
            a = add(a, r[i]); b = add(b, r[i + 1])
            c = add(c, r[i + 2]); d = add(d, r[i + 3])
            e = add(e, r[i + 4]); f = add(f, r[i + 5])
            g = add(g, r[i + 6]); h = add(h, r[i + 7])
            seedMix()
            m[i] = a; m[i + 1] = b; m[i + 2] = c; m[i + 3] = d
            m[i + 4] = e; m[i + 5] = f; m[i + 6] = g; m[i + 7] = h
            i += 8
        }
        i = 0
        while (i < 256) {
            a = add(a, m[i]); b = add(b, m[i + 1])
            c = add(c, m[i + 2]); d = add(d, m[i + 3])
            e = add(e, m[i + 4]); f = add(f, m[i + 5])
            g = add(g, m[i + 6]); h = add(h, m[i + 7])
            seedMix()
            m[i] = a; m[i + 1] = b; m[i + 2] = c; m[i + 3] = d
            m[i + 4] = e; m[i + 5] = f; m[i + 6] = g; m[i + 7] = h
            i += 8
        }

        prng()
        gnt = 256
    }

    /**
     * 等价于 csTimer `mathlib.setSeed(256, seedStr)`：
     * 播种后预推进 256 次 [random]（csTimer 在每次重置种子后都会这么做）。
     */
    fun seedWithPreAdvance(s: IntArray) {
        seed(s)
        repeat(256) { random() }
    }

    /** isaac.js prng(n)。 */
    private fun prng(n0: Int = 1) {
        var n = if (n0 == 0) 1 else abs(n0)
        while (n-- > 0) {
            cnt = add(cnt, 1)
            brs = add(brs, cnt)
            for (i in 0 until 256) {
                when (i and 3) {
                    0 -> acc = acc xor (acc shl 13)
                    1 -> acc = acc xor (acc ushr 6)
                    2 -> acc = acc xor (acc shl 2)
                    3 -> acc = acc xor (acc ushr 16)
                }
                acc = add(m[(i + 128) and 0xff], acc)
                val x = m[i]
                val y = add(m[(x ushr 2) and 0xff], add(acc, brs))
                m[i] = y
                brs = add(m[(y ushr 10) and 0xff], x)
                r[i] = brs
            }
        }
    }

    /** isaac.js rand()：先自减 gnt，为 0 时重新生成一批。 */
    private fun rand(): Int {
        val old = gnt
        gnt = old - 1
        if (old == 0) {
            prng()
            gnt = 255
        }
        return r[gnt]
    }

    override fun random(): Double {
        val hi = (rand() ushr 5).toDouble()
        val lo = (rand() ushr 6).toDouble()
        return (hi * 0x4000000 + lo) / 0x20000000000000L
    }

    private companion object {
        /** isaac.js 的黄金比例常量 0x9e3779b9。 */
        val GOLDEN = 0x9e3779b9.toInt()
    }
}