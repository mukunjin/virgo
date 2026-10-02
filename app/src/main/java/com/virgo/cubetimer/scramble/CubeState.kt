package com.virgo.cubetimer.scramble

import com.virgo.cubetimer.scramble.cube.CubieCube

/**
 * 把打乱序列应用到复原态，得到魔方贴纸状态（用于立方体展开图预览）。
 *
 * 移植自 csTimer `mathlib.js` 的 `CubieCube.moveCube` / `CubeMult`：
 * 六个基本转动（U/R/F/D/L/B）以角块/棱块置换表给出，其余转动由基本转动累乘得到。
 * 输出为 54 字符 URFDLB facelet 串，与 `CubieCube.toFaceCube()` 一致。
 */
object CubeState {

    /** 面序（对应 facelet 串的每 9 个字符）。 */
    const val FACES = "URFDLB"

    private val moveCube: Array<CubieCube> by lazy { buildMoveCube() }

    /** 生成打乱后的 54 字符 facelet 串。 */
    fun faceletsOf(scramble: String): String {
        val cube = CubieCube()
        for (m in parseMoves(scramble)) {
            val next = CubieCube()
            cubeMult(cube, moveCube[m], next)
            cube.init(next.ca, next.ea)
        }
        return cube.toFaceCube()
    }

    /**
     * 解析打乱串为 18 个基本转动编号：
     * `U R F D L B` 分别对应 `0/3/6/9/12/15`，其后 `""`/`2`/`'` 分别 `+0/+1/+2`。
     */
    fun parseMoves(scramble: String): List<Int> =
        scramble.split(' ').mapNotNull { token ->
            if (token.isEmpty()) return@mapNotNull null
            val face = FACES.indexOf(token[0])
            if (face < 0) return@mapNotNull null
            val turn = when (token.getOrNull(1)) {
                '2' -> 1
                '\'' -> 2
                else -> 0
            }
            face * 3 + turn
        }

    /** `CubieCube.CornMult` + `EdgeMult`（对应 `CubeMult`）。 */
    private fun cubeMult(a: CubieCube, b: CubieCube, prod: CubieCube) {
        for (corn in 0 until 8) {
            val idx = a.ca[b.ca[corn] and 7]
            val ori = ((idx shr 3) + (b.ca[corn] shr 3)) % 3
            prod.ca[corn] = (idx and 7) or (ori shl 3)
        }
        for (ed in 0 until 12) {
            prod.ea[ed] = a.ea[b.ea[ed] shr 1] xor (b.ea[ed] and 1)
        }
    }

    private fun buildMoveCube(): Array<CubieCube> {
        // 六个基本转动，取自 mathlib.moveCube[0/3/6/9/12/15]
        val base = arrayOf(
            CubieCube().init(
                intArrayOf(3, 0, 1, 2, 4, 5, 6, 7),
                intArrayOf(6, 0, 2, 4, 8, 10, 12, 14, 16, 18, 20, 22),
            ),
            CubieCube().init(
                intArrayOf(20, 1, 2, 8, 15, 5, 6, 19),
                intArrayOf(16, 2, 4, 6, 22, 10, 12, 14, 8, 18, 20, 0),
            ),
            CubieCube().init(
                intArrayOf(9, 21, 2, 3, 16, 12, 6, 7),
                intArrayOf(0, 19, 4, 6, 8, 17, 12, 14, 3, 11, 20, 22),
            ),
            CubieCube().init(
                intArrayOf(0, 1, 2, 3, 5, 6, 7, 4),
                intArrayOf(0, 2, 4, 6, 10, 12, 14, 8, 16, 18, 20, 22),
            ),
            CubieCube().init(
                intArrayOf(0, 10, 22, 3, 4, 17, 13, 7),
                intArrayOf(0, 2, 20, 6, 8, 10, 18, 14, 16, 4, 12, 22),
            ),
            CubieCube().init(
                intArrayOf(0, 1, 11, 23, 4, 5, 18, 14),
                intArrayOf(0, 2, 4, 23, 8, 10, 12, 21, 16, 18, 7, 15),
            ),
        )
        val moves = Array(18) { CubieCube() }
        for (axis in 0 until 6) {
            val b = base[axis]
            val at = axis * 3
            moves[at] = CubieCube().init(b.ca, b.ea)
            val twice = CubieCube()
            cubeMult(b, b, twice)
            moves[at + 1] = CubieCube().init(twice.ca, twice.ea)
            val thrice = CubieCube()
            cubeMult(moves[at + 1], b, thrice)
            moves[at + 2] = CubieCube().init(thrice.ca, thrice.ea)
        }
        return moves
    }
}