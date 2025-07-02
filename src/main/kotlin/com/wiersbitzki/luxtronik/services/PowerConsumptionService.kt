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
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.websocket.send
import kotlinx.coroutines.runBlocking
import nl.adaptivity.xmlutil.serialization.XML
import org.koin.core.annotation.Singleton

private const val HOST = "192.168.178.164"
private const val DEFAULT_PIN = 999999

private const val KWH_SUFFIX = " kWh"
private const val KW_SUFFIX = " kW"

@Singleton
class PowerConsumptionService {

    internal val logger = KtorSimpleLogger(PowerConsumptionService::class.java.simpleName)
    internal val client = HttpClient(CIO) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(XML)
        }
    }

    fun data() =
        runBlocking {
            val result = mutableMapOf<String, Map<String, Double>>()
            client.webSocket(
                method = HttpMethod.Get,
                host = HOST,
                port = 8214,
                path = "/",
                request = {
                    header(HttpHeaders.SecWebSocketProtocol, "Lux_WS")
                }
            ) {
                send("LOGIN;$DEFAULT_PIN")
                receiveDeserialized<Navigation>()
                    .let {
                        logger.debug("Fetched navigation: {}", it)

                        send("GET;${it.readNavigationId("Anlagenstatus")}")
                        receiveDeserialized<Content>()
                            .also { logger.debug("Fetched content: {}", it) }
                            .extractLeistungsaufnahmeAktuell()
                            .let { result.put("aktuell", it) }

                        send("GET;${it.readNavigationId("Leistungsaufnahme")}")
                        receiveDeserialized<Content>()
                            .also { logger.debug("Fetched content: {}", it) }
                            .extractLeistungsaufnahmeGesamt()
                            .let { result.put("gesamt", it) }
                }
            }
            return@runBlocking result
        }

    private fun Content.extractLeistungsaufnahmeAktuell(): Map<String, Double> {
        val betriebszustand = readValue("Betriebszustand")
        val leistungsaufnahme = readValue("Leistungsaufnahme", KW_SUFFIX).toDouble() * 1000
        return mapOf(
            "heizung" to if (betriebszustand == "Heiz") leistungsaufnahme else 0.0,
            "warmwasser" to if (betriebszustand == "WW") leistungsaufnahme else 0.0,
            "umwälzung" to if (betriebszustand != "Heiz" && betriebszustand != "WW") leistungsaufnahme else 0.0,
        ).also { logger.info("Leistungsaufnahme (Aktuell): {}", it) }
    }

    private fun Content.extractLeistungsaufnahmeGesamt() =
        mapOf(
            "heizung" to readValue("Heizung", KWH_SUFFIX).toDouble(),
            "warmwasser" to readValue("Warmwasser", KWH_SUFFIX).toDouble(),
            "gesamt" to readValue("Gesamt", KW_SUFFIX).toDouble()
        ).also { logger.info("Leistungsaufnahme (Gesamt): {}", it) }
}