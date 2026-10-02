package com.virgo.cubetimer.scramble.cube

/**
 * 移植自 csTimer mathlib.js 的 CubieCube（仅 333 打乱所需子集）。
 *
 * 角块 ca[8]：`朝向 << 3 | 角块序号`；棱块 ea[12]：`棱块序号 << 1 | 朝向`。
 * `ori` 为整体朝向索引，333 路径恒为 0（不触发 withOri 分支）。
 */
class CubieCube {

    var ca = IntArray(8) { it }
    var ea = IntArray(12) { it shl 1 }
    var ori = 0

    /** mathlib CubieCube.prototype.init。 */
    fun init(ca: IntArray, ea: IntArray): CubieCube {
        this.ca = ca.copyOf()
        this.ea = ea.copyOf()
        return this
    }

    /**
     * mathlib CubieCube.prototype.toPerm。
     * `withOri` 分支在 333 路径不会触发（ori 恒为 0），故未移植。
     */
    fun toPerm(
        cFacelet: Array<IntArray> = C_FACELET,
        eFacelet: Array<IntArray> = E_FACELET,
        withOri: Boolean = false,
    ): IntArray {
        check(!(withOri && ori != 0)) { "withOri 分支未移植（333 路径不需要）" }
        val f = IntArray(54) { it }
        for (c in 0 until 8) {
            val j = ca[c] and 0x7
            val o = ca[c] shr 3
            for (n in 0 until 3) {
                f[cFacelet[c][(n + o) % 3]] = cFacelet[j][n]
            }
        }
        for (e in 0 until 12) {
            val j = ea[e] shr 1
            val o = ea[e] and 1
            for (n in 0 until 2) {
                f[eFacelet[e][(n + o) % 2]] = eFacelet[j][n]
            }
        }
        return f
    }

    /** mathlib CubieCube.prototype.toFaceCube，输出 54 字符 URFDLB 面表示。 */
    fun toFaceCube(): String {
        val perm = toPerm()
        val sb = StringBuilder(54)
        for (i in 0 until 54) {
            sb.append(FACES[perm[i] / 9])
        }
        return sb.toString()
    }

    companion object {
        /** mathlib.CubieCube.toFaceCube 使用的面序。 */
        private const val FACES = "URFDLB"

        /** mathlib CubieCube.cFacelet。 */
        val C_FACELET: Array<IntArray> = arrayOf(
            intArrayOf(8, 9, 20),   // URF
            intArrayOf(6, 18, 38),  // UFL
            intArrayOf(0, 36, 47),  // ULB
            intArrayOf(2, 45, 11),  // UBR
            intArrayOf(29, 26, 15), // DFR
            intArrayOf(27, 44, 24), // DLF
            intArrayOf(33, 53, 42), // DBL
            intArrayOf(35, 17, 51), // DRB
        )

        /** mathlib CubieCube.eFacelet。 */
        val E_FACELET: Array<IntArray> = arrayOf(
            intArrayOf(5, 10),  // UR
            intArrayOf(7, 19),  // UF
            intArrayOf(3, 37),  // UL
            intArrayOf(1, 46),  // UB
            intArrayOf(32, 16), // DR
            intArrayOf(28, 25), // DF
            intArrayOf(30, 43), // DL
            intArrayOf(34, 52), // DB
            intArrayOf(23, 12), // FR
            intArrayOf(21, 41), // FL
            intArrayOf(50, 39), // BL
            intArrayOf(48, 14), // BR
        )
    }
}