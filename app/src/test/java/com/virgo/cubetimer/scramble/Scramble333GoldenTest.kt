package com.virgo.cubetimer.scramble

import com.virgo.cubetimer.scramble.rng.IsaacRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 333 打乱"全复刻"金标准测试。
 *
 * 资源 `scramble333_golden.txt` 由 `tools/gen-golden.js` 从 csTimer 原始 JS 生成：
 * 固定 256×uint16 种子后连续调用 `scramble_333.getRandomScramble()`。
 * Kotlin 移植版在相同种子下必须逐字符输出一致。
 */
class Scramble333GoldenTest {

    @Test
    fun matchesCsTimerGoldenVectors() {
        val lines = goldenLines()
        val seedHeader = lines.first()
        assertTrue("金标准首行应为 SEED 头", seedHeader.startsWith("SEED "))

        val seed = seedHeader.removePrefix("SEED ")
            .split(",")
            .map { it.trim().toInt() }
            .toIntArray()
        assertEquals(256, seed.size)

        val expected = lines.drop(1)
        assertTrue("金标准向量数量过少", expected.size >= 10)

        val rng = IsaacRandom()
        rng.seedWithPreAdvance(seed)
        val generator = Scramble333(rng)

        for (i in expected.indices) {
            assertEquals("第 $i 条打乱与 csTimer 不一致", expected[i], generator.getRandomScramble())
        }
    }

    private fun goldenLines(): List<String> {
        val stream = javaClass.classLoader!!.getResourceAsStream(GOLDEN_RESOURCE)
            ?: error("找不到金标准资源 $GOLDEN_RESOURCE")
        return stream.bufferedReader(Charsets.UTF_8)
            .readText()
            .trimEnd('\r', '\n')
            .split("\n")
            .map { it.trimEnd('\r') }
    }

    private companion object {
        const val GOLDEN_RESOURCE = "scramble333_golden.txt"
    }
}