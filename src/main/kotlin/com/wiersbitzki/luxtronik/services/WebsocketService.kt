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
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.seconds

private val TIMEOUT = 2.seconds

@Single
class WebsocketService(
    config: ApplicationConfig,
    private val host: String = config.tryGetString("ktor.service.websocket.host")!!,
    private val port: String = config.tryGetString("ktor.service.websocket.port")!!,
    private val pin: String = config.tryGetString("ktor.service.websocket.pin")!!
) : Logging {

    suspend fun retrieveData(
        block: suspend DefaultClientWebSocketSession.(result: MutableMap<String, Any>) -> Unit
    ): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        withTimeout(TIMEOUT) {
            with(HttpClient(CIO) {
                install(WebSockets) {
                    contentConverter = KotlinxWebsocketSerializationConverter(Json)
                }
            }) {
                logger.debug("Connecting to $host:$port...")
                try {
                    webSocket(
                        method = HttpMethod.Get,
                        host = host,
                        port = port.toInt(),
                        path = "/",
                        request = {
                            header(HttpHeaders.SecWebSocketProtocol, "Lux_WS")
                        }
                    ) {
                        try {
                            send("LOGIN;$pin").also { logger.debug("Sending login...") }
                            block(result).also { logger.debug("Running block.") }
                            close().also { logger.debug("Session closed.") }
                        } catch (e: Throwable) {
                            closeExceptionally(e)
                            throw e
                        }
                    }
                } catch (e: Throwable) {
                    logger.error("Catched exception while connected to $host:$port", e)
                    throw e
                } finally {
                    close().also { logger.debug("Connection to $host:$port closed") }
                }
            }
        }
        return result
    }
}