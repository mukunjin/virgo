package com.virgo.cubetimer.scramble.rng

/** mathlib.rn(n)：`~~(random() * n)`。 */
fun RandomSource.rn(n: Int): Int = (random() * n).toInt()

/** mathlib.rndEl(x)：`x[~~(random() * x.length)]`。 */
fun <T> RandomSource.rndEl(list: List<T>): T = list[(random() * list.size).toInt()]