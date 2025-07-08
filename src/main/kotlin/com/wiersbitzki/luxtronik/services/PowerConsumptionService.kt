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
import io.ktor.websocket.close
import io.ktor.websocket.send
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import nl.adaptivity.xmlutil.serialization.XML
import org.koin.core.annotation.Singleton
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

private const val HOST = "192.168.178.164"
private const val DEFAULT_PIN = 999999

private const val KWH_SUFFIX = " kWh"
private const val KW_SUFFIX = " kW"

@Singleton
class PowerConsumptionService {

    internal val logger = KtorSimpleLogger(PowerConsumptionService::class.java.simpleName)
    internal val client
        get() = HttpClient(CIO) {
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(XML)
            }
        }

    fun data(): Map<String, Map<String, Double>> =
        runBlocking {
            withTimeout(1.seconds) {
                fetchData()
            }
        }

    private suspend fun fetchData(): Map<String, Map<String, Double>> {
        val data = mutableMapOf<String, Map<String, Double>>()
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
                receiveDeserialized<Navigation>()
                    .let { navigation ->
                        send("GET;${navigation.anlagenstatusId}")
                        receiveDeserialized<Content>()
                            .let { content -> data.put("aktuell", content.extractLeistungsaufnahmeAktuell()) }
                        send("GET;${navigation.leistungsaufnahmeId}")
                        receiveDeserialized<Content>()
                            .let { content -> data.put("gesamt", content.extractLeistungsaufnahmeGesamt()) }
                    }
                close()
            }
            it.close()
        }
        return data
    }

    private val Navigation.anlagenstatusId
        get() = readNavigationId("Anlagenstatus")
            .also { logger.debug("Navigation: {}", this) }
    private val Navigation.leistungsaufnahmeId
        get() = readNavigationId("Leistungsaufnahme")
            .also { logger.debug("Navigation: {}", this) }

    private fun Content.extractLeistungsaufnahmeAktuell(): Map<String, Double> {
        val betriebszustand = readValue("Betriebszustand")
        val leistungsaufnahme = readValue("Leistungsaufnahme", KW_SUFFIX).toDouble().asFuzzyWatt()
        return mapOf(
            "heizung" to when {
                betriebszustand.isHeizung() -> leistungsaufnahme
                betriebszustand.isBlank() -> leistungsaufnahme / 2
                else -> 0.0
            },
            "warmwasser" to when {
                betriebszustand.isWarmwasser() -> leistungsaufnahme
                betriebszustand.isBlank() -> leistungsaufnahme / 2
                else -> 0.0
            }
        ).also {
            logger.debug("Content: {}", this)
        }.also {
            logger.info("Leistungsaufnahme (Aktuell): {}", it)
        }
    }

    private fun Double.asFuzzyWatt() =
        if (this > 0) this * 1000 + Random.nextInt(-5, 5) else 0.0

    private fun String.isHeizung() = this == "Heizen"
    private fun String.isWarmwasser() = this == "WW"

    private fun Content.extractLeistungsaufnahmeGesamt() =
        mapOf(
            "heizung" to readValue("Heizung", KWH_SUFFIX).toDouble(),
            "warmwasser" to readValue("Warmwasser", KWH_SUFFIX).toDouble(),
        ).also {
            logger.debug("Content: {}", this)
        }.also {
            logger.info("Leistungsaufnahme (Gesamt): {}", it)
        }
}