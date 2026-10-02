package com.virgo.cubetimer.scramble.rng

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ISAAC 随机数金标准测试：验证 RNG 与 csTimer 逐位一致。
 *
 * 该测试把"RNG 偏差"与"求解器偏差"分离开，便于定位问题。
 */
class IsaacGoldenTest {

    @Test
    fun matchesCsTimerRandomStream() {
        val lines = readLines("isaac_golden.txt")
        val seed = lines.first().removePrefix("SEED ")
            .split(",")
            .map { it.trim().toInt() }
            .toIntArray()
        val expected = lines.drop(1).map { it.trim() }

        val rng = IsaacRandom()
        rng.seedWithPreAdvance(seed)

        for (i in expected.indices) {
            assertEquals(
                "第 $i 个随机数与 csTimer 不一致",
                expected[i].toDouble(),
                rng.random(),
                0.0,
            )
        }
    }

    private fun readLines(name: String): List<String> {
        val stream = javaClass.classLoader!!.getResourceAsStream(name)
            ?: error("找不到金标准资源 $name")
        return stream.bufferedReader(Charsets.UTF_8).readText().trimEnd('\n').split("\n")
    }
}