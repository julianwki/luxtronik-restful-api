package com.wiersbitzki.luxtronik.services

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import nl.adaptivity.xmlutil.serialization.XML
import org.koin.core.annotation.Singleton
import kotlin.time.Duration.Companion.seconds

private const val HOST = "192.168.178.164"
private const val DEFAULT_PIN = 999999
private val TIMEOUT = 1.seconds

@Singleton
class WebsocketService {

    private val client
        get() = HttpClient(CIO) {
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(XML)
            }
        }

    fun execute(block: suspend DefaultClientWebSocketSession.() -> Unit) {
        runBlocking {
            withTimeout(TIMEOUT) {
                client.let {
                    it.webSocket(
                        method = HttpMethod.Get,
                        host = HOST,
                        port = 8214,
                        path = "/",
                        request = {
                            header(HttpHeaders.SecWebSocketProtocol, "Lux_WS")
                        }
                    ) {
                        send("LOGIN;$DEFAULT_PIN")
                        block()
                        close()
                    }
                    it.close()
                }
            }
        }
    }

}