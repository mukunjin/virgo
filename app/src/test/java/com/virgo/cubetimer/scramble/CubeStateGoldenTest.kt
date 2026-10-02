package com.virgo.cubetimer.scramble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 立方体展开图所需的贴纸状态金标准测试。
 *
 * 资源 `cube_golden.txt` 的每一行为 `打乱|贴纸串`，由 csTimer 原始 `mathlib.js`
 * 的 `CubieCube.moveCube` / `CubeMult` 计算得出（Node 脚本生成）。
 * Kotlin 移植版必须逐字符一致，否则展开图会与 csTimer 不符。
 */
class CubeStateGoldenTest {

    private val solved = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB"

    @Test
    fun matchesCsTimerGoldenVectors() {
        val lines = goldenLines()
        assertTrue("金标准向量数量过少", lines.size >= 10)

        for ((i, line) in lines.withIndex()) {
            val sep = line.indexOf('|')
            assertTrue("第 $i 行缺少分隔符", sep >= 0)
            val scramble = line.substring(0, sep)
            val expected = line.substring(sep + 1)
            assertEquals(54, expected.length)
            assertEquals(
                "打乱「$scramble」的贴纸状态与 csTimer 不一致",
                expected,
                CubeState.faceletsOf(scramble),
            )
        }
    }

    /** 空打乱（复原态）与基本旋转的自洽性检查。 */
    @Test
    fun basicProperties() {
        assertEquals(solved, CubeState.faceletsOf(""))

        // 每个面转 4 次回到复原态
        for (face in CubeState.FACES) {
            assertEquals(
                "连续 4 次 $face 应回到复原态",
                solved,
                CubeState.faceletsOf("$face $face $face $face"),
            )
        }

        // 每个面与其逆转动互相抵消
        for (face in CubeState.FACES) {
            assertEquals(solved, CubeState.faceletsOf("$face $face'"))
            assertEquals(solved, CubeState.faceletsOf("$face' $face"))
            assertEquals(solved, CubeState.faceletsOf("$face $face $face' $face'"))
        }

        // 打乱与其逆序互逆后回到复原态
        val scramble = "R U F' L2 D B' U2 R' F"
        val inverse = scramble.split(' ').asReversed().joinToString(" ") { invert(it) }
        assertEquals(solved, CubeState.faceletsOf("$scramble $inverse"))
    }

    private fun invert(move: String): String = when (move.last()) {
        '\'' -> move.dropLast(1)
        '2' -> move
        else -> "$move'"
    }

    private fun goldenLines(): List<String> {
        val stream = javaClass.classLoader!!.getResourceAsStream(GOLDEN_RESOURCE)
            ?: error("找不到金标准资源 $GOLDEN_RESOURCE")
        return stream.bufferedReader(Charsets.UTF_8)
            .readText()
            .trimEnd('\n')
            .split("\n")
            .filter { it.isNotBlank() }
    }

    private companion object {
        const val GOLDEN_RESOURCE = "cube_golden.txt"
    }
}