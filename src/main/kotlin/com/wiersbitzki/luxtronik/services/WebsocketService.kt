package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Logging
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.websocket.close
import io.ktor.websocket.send
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import nl.adaptivity.xmlutil.serialization.XML
import org.koin.core.annotation.Singleton
import kotlin.time.Duration.Companion.seconds

private const val HOST = "192.168.178.164"
private const val PORT = 8214
private const val DEFAULT_PIN = 999999
private val TIMEOUT = 1.seconds

@Singleton
class WebsocketService : Logging {

    private val client
        get() = HttpClient(CIO) {
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(XML)
            }
        }

    fun execute(block: suspend DefaultClientWebSocketSession.() -> Unit) {
        runBlocking {
            withTimeout(TIMEOUT) {
                client
                    .also { logger.info("Connecting to $HOST:$PORT...") }
                    .let {
                        it.webSocket(
                            method = HttpMethod.Get,
                            host = HOST,
                            port = PORT,
                            path = "/",
                            request = {
                                header(HttpHeaders.SecWebSocketProtocol, "Lux_WS")
                            }
                        ) {
                            logger.info("Sending websocket login...")
                                .also { send("LOGIN;$DEFAULT_PIN") }
                            block()
                            logger.info("Closing websocket...")
                                .also { close() }
                        }
                        it.close()
                            .also { logger.info("Connection to $HOST:$PORT closed") }
                    }
            }
        }
    }

}