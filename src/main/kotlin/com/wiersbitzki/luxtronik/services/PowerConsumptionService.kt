package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Content
import com.wiersbitzki.luxtronik.DataService
import com.wiersbitzki.luxtronik.Navigation
import com.wiersbitzki.luxtronik.asFuzzyWatt
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.websocket.send
import org.koin.core.annotation.Singleton

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
        }.also { logger.info(it.toString()) }

    private val Navigation.anlagenstatusId
        get() = readNavigationId("Informationen", "Anlagenstatus")
    private val Navigation.leistungsaufnahmeId
        get() = readNavigationId("Informationen", "Energiemonitor", "Leistungsaufnahme")

    private fun Content.extractLeistungsaufnahmeAktuell(): Map<String, Double> {
        val betriebszustand = readValue("Betriebszustand")
        val leistungsaufnahme = readValue("Leistungsaufnahme", KW_SUFFIX).asFuzzyWatt()
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
                betriebszustand.isNotBlank() -> leistungsaufnahme
                else -> 0.0
            }
        )
    }

    private fun String.isHeizung() = this == "Heizen"
    private fun String.isWarmwasser() = this == "WW"

    private fun Content.extractLeistungsaufnahmeGesamt() =
        mapOf(
            "heizung" to readValue("Heizung", KWH_SUFFIX).toDouble(),
            "warmwasser" to readValue("Warmwasser", KWH_SUFFIX).toDouble(),
            "gesamt" to readValue("Gesamt", KWH_SUFFIX).toDouble()
        )
}