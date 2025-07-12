package com.wiersbitzki.luxtronik

import java.math.RoundingMode
import kotlin.random.Random

fun Double.round(scale: Int = 2) = toBigDecimal()
    .setScale(scale, RoundingMode.HALF_UP)
    .toDouble()

fun String.asFuzzyWatt() = toDouble()
    .let { if (it > 0) it * 1000 + Random.nextInt(-5, 5) else 0.0 }
