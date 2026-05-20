package com.wiersbitzki.luxtronik

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.websocket.send

fun interface DataService : Logging {
    suspend fun data(): Map<String, Any>
    suspend fun DefaultClientWebSocketSession.receiveContent(id: String): Content {
        send("GET;${id}")
        return receiveDeserialized<Content>()
    }

}