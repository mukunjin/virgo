package com.virgo.cubetimer.scramble

import com.virgo.cubetimer.scramble.cube.CubeMath
import com.virgo.cubetimer.scramble.cube.CubieCube
import com.virgo.cubetimer.scramble.min2phase.Search
import com.virgo.cubetimer.scramble.rng.IsaacRandom
import com.virgo.cubetimer.scramble.rng.RandomSource
import com.virgo.cubetimer.scramble.rng.rn
import com.virgo.cubetimer.scramble.rng.rndEl

/**
 * 移植自 csTimer `scramble_333_edit.js` 的三阶（333）打乱生成器。
 *
 * csTimer 的 333 打乱是**随机态**打乱：先随机构造一个合法魔方状态，
 * 再用 min2phase 求解并取**逆解**作为打乱。因此本类必须与 JS 逐行一致，
 * 尤其是随机数的消耗顺序：
 *
 * 1. `fixOri(eo, 12, 2)`  -> 11 次 `rn(2)`
 * 2. `fixOri(co, 12, 3)`  -> 11 次 `rn(3)`
 * 3. `fixPerm(ep, 12, -1)` -> 12 次 `rn(cntU)`，cntU 由 12 递减到 1
 * 4. `fixPerm(cp, 12, parity)` -> 12 次 `rn(cntU)`
 * 5. `rndEl(rndpre)` / `rndEl(rndapp)`（默认空后缀，但仍各消耗 1 次随机数）
 *
 * 任意偏差都会导致打乱与 csTimer 不一致。
 */
class Scramble333(private val rng: RandomSource) {

    private val search = Search()

    /** scramble_333_edit.js 中 `getRandomScramble()`（仅 333）。 */
    fun getRandomScramble(): String =
        getAnyScramble(EP_MASK, EO_MASK, CP_MASK, CO_MASK)

    private fun getAnyScramble(epMask: Long, eoMask: Long, cpMask: Long, coMask: Long): String {
        val epBase = parseMask(epMask, 12)
        val eoBase = parseMask(eoMask, 12)
        val cpBase = parseMask(cpMask, 8)
        val coBase = parseMask(coMask, 8)

        var solution = ""
        do {
            val eo = eoBase.copyOf()
            val ep = epBase.copyOf()
            val co = coBase.copyOf()
            val cp = cpBase.copyOf()

            val neo = fixOri(eo, cntU(eo), 2)
            val nco = fixOri(co, cntU(co), 3)

            var nep: Int
            var ncp: Int
            var ue = cntU(ep)
            var uc = cntU(cp)
            if (ue == 1) {
                fixPerm(ep, ue, -1)
                ue = 0
            }
            if (uc == 1) {
                fixPerm(cp, uc, -1)
                uc = 0
            }
            if (ue == 0 && uc == 0) {
                nep = CubeMath.getNPerm(ep, 12)
                ncp = CubeMath.getNPerm(cp, 8)
            } else if (ue != 0 && uc == 0) {
                ncp = CubeMath.getNPerm(cp, 8)
                nep = fixPerm(ep, ue, CubeMath.getNParity(ncp, 8))
            } else if (ue == 0 && uc != 0) {
                nep = CubeMath.getNPerm(ep, 12)
                ncp = fixPerm(cp, uc, CubeMath.getNParity(nep, 12))
            } else {
                nep = fixPerm(ep, ue, -1)
                ncp = fixPerm(cp, uc, CubeMath.getNParity(nep, 12))
            }
            if (ncp + nco + nep + neo == 0) {
                continue
            }

            // 默认前后缀为空（emptysuff = [[]]），但 rndEl 仍各消耗一次随机数。
            rng.rndEl(EMPTY_SUFFIXES)
            rng.rndEl(EMPTY_SUFFIXES)

            val cc = CubieCube()
            for (i in 0 until 12) {
                cc.ea[i] = (ep[i] shl 1) or eo[i]
            }
            for (i in 0 until 8) {
                cc.ca[i] = (co[i] shl 3) or cp[i]
            }

            val posit = cc.toFaceCube()
            solution = search.solution(posit, MAX_DEPTH, PROBE_MAX, PROBE_MIN, VERBOSE, null, null)
        } while (solution.length <= 3)

        return solution.replace(SPACES, " ")
    }

    /** scramble_333_edit.js 的 cntU(b)。 */
    private fun cntU(b: IntArray): Int {
        var c = 0
        for (v in b) {
            if (v == -1) c++
        }
        return c
    }

    /** scramble_333_edit.js 的 fixOri(arr, cntU, base)。 */
    private fun fixOri(arr: IntArray, cntU0: Int, base: Int): Int {
        var cntU = cntU0
        var sum = 0
        for (v in arr) {
            if (v != -1) sum += v
        }
        sum %= base

        var idx = 0
        for (i in 0 until arr.size - 1) {
            if (arr[i] == -1) {
                if (cntU-- == 1) {
                    arr[i] = ((base shl 4) - sum) % base
                } else {
                    arr[i] = rng.rn(base)
                    sum += arr[i]
                }
            }
            idx *= base
            idx += arr[i]
        }
        if (cntU == 1) {
            // JS: arr.splice(-1, 1, ((base << 4) - sum) % base)
            arr[arr.size - 1] = ((base shl 4) - sum) % base
        }
        return idx
    }

    /** scramble_333_edit.js 的 fixPerm(arr, cntU, parity)。 */
    private fun fixPerm(arr: IntArray, cntU0: Int, parity: Int): Int {
        var cntU = cntU0
        val pool = IntArray(12) { it }
        for (v in arr) {
            if (v != -1) pool[v] = -1
        }
        var j = 0
        for (i in pool.indices) {
            if (pool[i] != -1) {
                pool[j++] = pool[i]
            }
        }
        var last = 0
        var i = 0
        while (i < arr.size && cntU > 0) {
            if (arr[i] == -1) {
                val r = rng.rn(cntU)
                arr[i] = pool[r]
                for (k in r until 11) {
                    pool[k] = pool[k + 1]
                }
                if (cntU-- == 2) last = i
            }
            i++
        }
        if (CubeMath.getNParity(CubeMath.getNPerm(arr, arr.size), arr.size) == 1 - parity) {
            val temp = arr[i - 1]
            arr[i - 1] = arr[last]
            arr[last] = temp
        }
        return CubeMath.getNPerm(arr, arr.size)
    }

    /**
     * scramble_333_edit.js 的 parseMask(arr, length)。
     * JS 用浮点除法 `arr /= 16` 配合 `arr & 0xf`（先 ToInt32），此处严格对齐。
     */
    private fun parseMask(mask: Long, length: Int): IntArray {
        var a = mask.toDouble()
        val ret = IntArray(length)
        for (i in 0 until length) {
            val v = a.toLong().toInt() and 0xf
            ret[i] = if (v == 15) -1 else v
            a /= 16.0
        }
        return ret
    }

    companion object {
        private val EMPTY_SUFFIXES: List<List<Int>> = listOf(emptyList())

        /** `getRandomScramble()` 传入的四段掩码（全 -1，即完全随机态）。 */
        private const val EP_MASK = 0xffffffffffffL
        private const val EO_MASK = 0xffffffffffffL
        private const val CP_MASK = 0xffffffffL
        private const val CO_MASK = 0xffffffffL

        /** `search.solution(posit, 21, 1e9, 50, 2, ...)` 的固定参数。 */
        private const val MAX_DEPTH = 21
        private const val PROBE_MAX = 1_000_000_000
        private const val PROBE_MIN = 50
        private const val VERBOSE = 2

        private val SPACES = Regex(" +")

        /** 生成 256 个 uint16 种子，等价于 csTimer 的 `crypto.getRandomValues(Uint16Array(256))`。 */
        fun newSecureSeed(): IntArray {
            val rnd = java.security.SecureRandom()
            return IntArray(256) { rnd.nextInt(0x10000) }
        }

        /** 创建生产用打乱器：SecureRandom 播种并预推进。 */
        fun create(): Scramble333 {
            val rng = IsaacRandom()
            rng.seedWithPreAdvance(newSecureSeed())
            return Scramble333(rng)
        }
    }
}