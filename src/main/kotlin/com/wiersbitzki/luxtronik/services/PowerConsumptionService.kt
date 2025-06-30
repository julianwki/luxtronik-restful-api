package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Content
import com.wiersbitzki.luxtronik.Navigation
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.websocket.send
import kotlinx.coroutines.runBlocking
import nl.adaptivity.xmlutil.serialization.XML
import org.koin.core.annotation.Singleton

private const val KWH_SUFFIX = " kWh"
private const val DEFAULT_PIN = 999999

@Singleton
class PowerConsumptionService {

    val client = HttpClient(CIO) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(XML)
        }
    }

    fun data(): Map<String, Double> =
        runBlocking {
            val result = mutableMapOf<String, Double>()
            client.webSocket(
                method = HttpMethod.Get,
                host = "novelan",
                port = 8214,
                path = "/",
                request = {
                    header(HttpHeaders.SecWebSocketProtocol, "Lux_WS")
                }
            ) {
                send("LOGIN;$DEFAULT_PIN")
                val navigation = receiveDeserialized<Navigation>()

                send("GET;${navigation.leistungsaufnahme}")
                val content = receiveDeserialized<Content>()

                with(content) {
                    result.putAll(
                        mapOf(
                            "heizung" to heizung,
                            "warmwasser" to warmwasser,
                            "gesamt" to heizung + warmwasser
                        )
                    )
                }
            }
            return@runBlocking result
        }

    private val Navigation.leistungsaufnahme
        get() = findNavigationItemWithName("Leistungsaufnahme").id
    private val Content.heizung
        get() = findDataItemWithName("Heizung")
            .value
            .substringBefore(KWH_SUFFIX)
            .toDouble()
    private val Content.warmwasser
        get() = findDataItemWithName("Warmwasser")
            .value
            .substringBefore(KWH_SUFFIX)
            .toDouble()
}