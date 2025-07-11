package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Content
import com.wiersbitzki.luxtronik.Navigation
import io.ktor.client.plugins.websocket.*
import io.ktor.util.logging.*
import io.ktor.websocket.*
import org.koin.core.annotation.Singleton
import kotlin.random.Random

private const val KWH_SUFFIX = "kWh"
private const val KW_SUFFIX = "kW"

@Singleton
class PowerConsumptionService(
    private val websocketService: WebsocketService
) {

    internal val logger = KtorSimpleLogger(PowerConsumptionService::class.java.simpleName)

    fun data(): Map<String, Map<String, Double>> {
        val data = mutableMapOf<String, Map<String, Double>>()
        websocketService.execute {
            receiveDeserialized<Navigation>()
                .let { navigation ->
                    send("GET;${navigation.anlagenstatusId}")
                    receiveDeserialized<Content>()
                        .let { content -> data.put("aktuell", content.extractLeistungsaufnahmeAktuell()) }
                    send("GET;${navigation.leistungsaufnahmeId}")
                    receiveDeserialized<Content>()
                        .let { content -> data.put("gesamt", content.extractLeistungsaufnahmeGesamt()) }
                }
        }
        return data
    }

    private val Navigation.anlagenstatusId
        get() = readNavigationId("Informationen", "Anlagenstatus")
            .also { logger.debug("Navigation: {}", this) }
    private val Navigation.leistungsaufnahmeId
        get() = readNavigationId("Informationen", "Energiemonitor", "Leistungsaufnahme")
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