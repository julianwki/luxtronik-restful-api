package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Content
import com.wiersbitzki.luxtronik.Navigation
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.util.logging.*
import io.ktor.websocket.*
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
                val navigation = receiveDeserialized<Navigation>()
                    .also { logger.info("Fetched navigation: {}", it) }

                with(navigation) {
                    send("GET;${readNavigationId("Anlagenstatus")}")
                    result.put(
                        "aktuell",
                        receiveDeserialized<Content>()
                            .also { logger.info("Fetched content: {}", it) }
                            .extractLeistungsaufnahmeAktuell()
                    )

                    send("GET;${readNavigationId("Leistungsaufnahme")}")
                    result.put(
                        "gesamt", receiveDeserialized<Content>()
                            .also { logger.info("Fetched content: {}", it) }
                            .extractLeistungsaufnahmeGesamt()
                    )
                }
            }
            return@runBlocking result
        }

    private fun Content.extractLeistungsaufnahmeAktuell(): Map<String, Double> {
        val betriebszustand = readValue("Betriebszustand")
        val leistungsaufnahme = readValue("Leistungsaufnahme", KW_SUFFIX).toDouble()
        return mapOf(
            "heizung" to if (betriebszustand == "Heiz") leistungsaufnahme else 0.0,
            "warmwasser" to if (betriebszustand == "WW") leistungsaufnahme else 0.0,
        ).also { logger.info("Leistungsaufnahme (Aktuell): {}", it) }
    }

    private fun Content.extractLeistungsaufnahmeGesamt() =
        mapOf(
            "heizung" to readValue("Heizung", KWH_SUFFIX).toDouble(),
            "warmwasser" to readValue("Warmwasser", KWH_SUFFIX).toDouble(),
        ).also { logger.info("Leistungsaufnahme (Gesamt): {}", it) }

    private fun Navigation.readNavigationId(name: String) =
        findNavigationItemWithName(name).id

    private fun Content.readValue(name: String, suffix: String = "") =
        findDataItemWithName(name)
            .value
            .let { if (suffix != "") it.substringBefore(suffix) else it }
}