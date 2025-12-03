package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Logging
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.config.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import nl.adaptivity.xmlutil.serialization.XML
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
                contentConverter = KotlinxWebsocketSerializationConverter(XML)
            }
        }

    fun retrieveData(block: suspend DefaultClientWebSocketSession.(result: MutableMap<String, Any>) -> Unit): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        runBlocking {
            withTimeout(TIMEOUT) {
                client
                    .also { logger.debug("Connecting to $host:$port...") }
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
                            logger.debug("Sending login...")
                                .also { send("LOGIN;$pin") }
                            block(result)
                            logger.debug("Closing websocket...")
                                .also { close() }
                        }
                        it.close()
                            .also { logger.debug("Connection to $host:$port closed") }
                    }
            }
        }
        return result
    }
}