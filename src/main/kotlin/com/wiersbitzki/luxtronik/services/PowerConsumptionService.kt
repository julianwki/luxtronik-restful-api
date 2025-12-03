package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Content
import com.wiersbitzki.luxtronik.DataService
import com.wiersbitzki.luxtronik.Navigation
import com.wiersbitzki.luxtronik.asFuzzyWatt
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import org.koin.core.annotation.Single

private const val KWH_SUFFIX = "kWh"
private const val KW_SUFFIX = "kW"

@Single
class PowerConsumptionService(
    private val websocketService: WebsocketService
) : DataService {

    override fun data(): Map<String, Any> =
        websocketService.retrieveData { result ->
            receiveDeserialized<Navigation>()
                .let {
                    send("GET;${it.anlagenstatusId}")
                    result["aktuell"] = receiveDeserialized<Content>().extractLeistungsaufnahmeAktuell()
                    send("GET;${it.leistungsaufnahmeId}")
                    result["gesamt"] = receiveDeserialized<Content>().extractLeistungsaufnahmeGesamt()
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
            "heizung" to leistungsaufnahme.heizung(betriebszustand),
            "warmwasser" to leistungsaufnahme.warmwasser(betriebszustand),
            "gesamt" to leistungsaufnahme
        )
    }

    private fun Double.heizung(betriebszustand: String) = when {
        betriebszustand.isHeizung -> this
        betriebszustand.isWarmwasser -> 0.0
        else -> this / 2
    }

    private fun Double.warmwasser(betriebszustand: String) = when {
        betriebszustand.isWarmwasser -> this
        betriebszustand.isHeizung -> 0.0
        else -> this / 2
    }

    private val String.isHeizung
        get() = this.equals("Heizen", ignoreCase = true)
    private val String.isWarmwasser
        get() = this.equals("WW", ignoreCase = true)

    private fun Content.extractLeistungsaufnahmeGesamt() =
        mapOf(
            "heizung" to readValue("Heizung", KWH_SUFFIX).toDouble(),
            "warmwasser" to readValue("Warmwasser", KWH_SUFFIX).toDouble(),
            "gesamt" to readValue("Gesamt", KWH_SUFFIX).toDouble()
        )
}