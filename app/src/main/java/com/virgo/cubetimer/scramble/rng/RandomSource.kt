package com.virgo.cubetimer.scramble.rng

/**
 * 随机源抽象。csTimer 中为 `mathlib` 的 `randGen`（内部绑定 isaac）。
 * 生产环境用 [IsaacRandom]，测试可注入固定种子。
 */
interface RandomSource {
    /** 返回 [0, 1) 的浮点数，等价于 csTimer 的 `isaac.random()`。 */
    fun random(): Double
}