package com.wiersbitzki.luxtronik

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.websocket.send
import java.math.RoundingMode
import kotlin.random.Random

fun Double.round(scale: Int = 2) = toBigDecimal()
    .setScale(scale, RoundingMode.HALF_UP)
    .toDouble()

fun String.asFuzzyWatt() = toDouble()
    .let { if (it > 0) it * 1000 + Random.nextInt(-5, 5) else 0.0 }

suspend fun DefaultClientWebSocketSession.receiveContent(id: String): Content {
    send("GET;${id}")
    return receiveDeserialized<Content>()
}