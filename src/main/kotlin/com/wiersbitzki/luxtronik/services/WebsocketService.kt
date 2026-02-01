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
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.tryGetString
import io.ktor.websocket.close
import io.ktor.websocket.closeExceptionally
import io.ktor.websocket.send
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.seconds

private val TIMEOUT = 1.seconds

@Single
class WebsocketService(
    config: ApplicationConfig
) : Logging {

    private val host = config.tryGetString("ktor.service.websocket.host")!!
    private val port = config.tryGetString("ktor.service.websocket.port")!!.toInt()
    private val pin = config.tryGetString("ktor.service.websocket.pin")!!

    private val client
        get() = HttpClient(CIO) {
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(Json)
            }
        }

    fun retrieveData(block: suspend DefaultClientWebSocketSession.(result: MutableMap<String, Any>) -> Unit): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        runBlocking {
            withTimeout(TIMEOUT) {
                logger.debug("Connecting to $host:$port...")
                try {
                    client.webSocket(
                        method = HttpMethod.Get,
                        host = host,
                        port = port,
                        path = "/",
                        request = {
                            header(HttpHeaders.SecWebSocketProtocol, "Lux_WS")
                        }
                    ) {
                        try {
                            send("LOGIN;$pin").also {
                                logger.debug("Sending login...")
                            }
                            block(result)
                            close().also {
                                logger.debug("Closing websocket...")
                            }
                        } catch (e: Throwable) {
                            closeExceptionally(e).also {
                                logger.error("Closing websocket with exception: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Catched exception while connected to $host:$port: ${e.message}")
                } finally {
                    client.close().also {
                        logger.debug("Connection to $host:$port closed")
                    }
                }
            }
        }
        return result
    }
}