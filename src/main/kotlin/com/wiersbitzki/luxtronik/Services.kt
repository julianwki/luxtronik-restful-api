package com.wiersbitzki.luxtronik

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.websocket.send

interface DataService : Logging {
    suspend fun data(): Map<String, Any>
    suspend fun DefaultClientWebSocketSession.receiveContent(id: String): Content {
        send("GET;${id}")
        return receiveDeserialized<Content>()
    }
}

interface SwitchService : Logging {
    fun activate(function: String, register: Int)
    fun deactivate(function: String, register: Int)
    fun switch(function: String, register: Int, onOff: OnOff)
    fun read(function: String, register: Int): Int
    fun readInput(function: String, register: Int): Int
    fun write(function: String, register: Int, value: Int)
}

enum class OnOff(
    val value: Int
) {
    ON(1),
    OFF(0)
}

interface AdjustmentService : Logging {
    suspend fun adjust()
}
