package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Content
import com.wiersbitzki.luxtronik.Navigation
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.websocket.send
import org.koin.core.annotation.Singleton
import kotlin.random.Random

private const val KWH_SUFFIX = "kWh"
private const val KW_SUFFIX = "kW"

@Singleton
class PowerConsumptionService(
    private val websocketService: WebsocketService
) : DataService {

    override fun data(): Map<String, Any> =
        websocketService.retrieveData { result ->
            receiveDeserialized<Navigation>()
                .let { navigation ->
                    send("GET;${navigation.anlagenstatusId}")
                    receiveDeserialized<Content>()
                        .let { content ->
                            result.put("aktuell", content.extractLeistungsaufnahmeAktuell())
                        }
                    send("GET;${navigation.leistungsaufnahmeId}")
                    receiveDeserialized<Content>()
                        .let { content ->
                            result.put("gesamt", content.extractLeistungsaufnahmeGesamt())
                        }
                }
        }.also { logger.info("$it") }

    private val Navigation.anlagenstatusId
        get() = readNavigationId("Informationen", "Anlagenstatus")
            .also { logger.debug("Navigation: {}", this) }
    private val Navigation.leistungsaufnahmeId
        get() = readNavigationId("Informationen", "Energiemonitor", "Leistungsaufnahme")
            .also { logger.debug("Navigation: {}", this) }

    private fun Content.extractLeistungsaufnahmeAktuell(): Map<String, Double> {
        val betriebszustand = readValue("Betriebszustand")
            .also { logger.info("Betriebszustand: {}", it.ifEmpty { "\"\"" }) }
        val leistungsaufnahme = readValue("Leistungsaufnahme", KW_SUFFIX)
            .also { logger.info("Leistungsaufnahme: {}", it) }
            .asFuzzyWatt()
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
            },
            "gesamt" to when {
                betriebszustand.isWarmwasser() -> leistungsaufnahme
                betriebszustand.isHeizung() -> leistungsaufnahme
                else -> 0.0
            }
        ).also {
            logger.debug("Content: {}", this)
        }
    }

    private fun String.asFuzzyWatt() = toDouble()
        .let { if (it > 0) it * 1000 + Random.nextInt(-5, 5) else 0.0 }

    private fun String.isHeizung() = this == "Heizen"
    private fun String.isWarmwasser() = this == "WW"

    private fun Content.extractLeistungsaufnahmeGesamt() =
        mapOf(
            "heizung" to readValue("Heizung", KWH_SUFFIX)
                .also { logger.info("Heizung: {}", it) }
                .toDouble(),
            "warmwasser" to readValue("Warmwasser", KWH_SUFFIX)
                .also { logger.info("Warmwasser: {}", it) }
                .toDouble(),
            "gesamt" to readValue("Gesamt", KWH_SUFFIX)
                .also { logger.info("Gesamt: {}", it) }
                .toDouble()
        ).also {
            logger.debug("Content: {}", this)
        }
}