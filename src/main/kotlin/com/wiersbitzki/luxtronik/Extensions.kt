package com.wiersbitzki.luxtronik

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.websocket.send
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

fun Double.round(scale: Int = 2) = toBigDecimal()
    .setScale(scale, RoundingMode.HALF_UP)
    .toDouble()

suspend fun DefaultClientWebSocketSession.receiveContent(id: String): Content {
    send("GET;${id}")
    return receiveDeserialized<Content>()
}

fun Pair<Double, Double>.cop() = if (second != 0.0) (first / second).round() else 0.0
