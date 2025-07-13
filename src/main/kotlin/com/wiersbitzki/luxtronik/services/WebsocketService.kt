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
import io.ktor.websocket.send
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import nl.adaptivity.xmlutil.serialization.XML
import org.koin.core.annotation.Singleton
import kotlin.time.Duration.Companion.seconds

private val TIMEOUT = 1.seconds

@Singleton
class WebsocketService(
    config: ApplicationConfig
) : Logging {

    private val host = config.tryGetString("ktor.service.websocket.host")!!
    private val port = config.tryGetString("ktor.service.websocket.port")!!.toInt()
    private val pin = config.tryGetString("ktor.service.websocket.pin")!!

    private val client
        get() = HttpClient(CIO) {
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(XML)
            }
        }

    fun retrieveData(block: suspend DefaultClientWebSocketSession.(result: MutableMap<String, Any>) -> Unit): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        runBlocking {
            withTimeout(TIMEOUT) {
                client
                    .also { logger.info("Connecting to $host:$port...") }
                    .let {
                        it.webSocket(
                            method = HttpMethod.Get,
                            host = host,
                            port = port,
                            path = "/",
                            request = {
                                header(HttpHeaders.SecWebSocketProtocol, "Lux_WS")
                            }
                        ) {
                            logger.info("Sending login...")
                                .also { send("LOGIN;$pin") }
                            block(result)
                            logger.info("Closing websocket...")
                                .also { close() }
                        }
                        it.close()
                            .also { logger.info("Connection to $host:$port closed") }
                    }
            }
        }
        return result
    }
}