package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Content
import com.wiersbitzki.luxtronik.DataService
import com.wiersbitzki.luxtronik.Navigation
import com.wiersbitzki.luxtronik.asFuzzyWatt
import com.wiersbitzki.luxtronik.receiveContent
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.websocket.send
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
                    val anlagenstatus = receiveContent(it.anlagenstatusId)
                    result["aktuell"] = extractLeistungsaufnahmeAktuell(anlagenstatus)
                    val leistungsaufnahme = receiveContent(it.leistungsaufnahmeId)
                    result["gesamt"] = extractLeistungsaufnahmeGesamt(leistungsaufnahme)
                }
        }.also { logger.info(it.toString()) }

    private val Navigation.anlagenstatusId
        get() = readNavigationId("Informationen", "Anlagenstatus")
    private val Navigation.leistungsaufnahmeId
        get() = readNavigationId("Informationen", "Energiemonitor", "Leistungsaufnahme")

    private fun extractLeistungsaufnahmeAktuell(anlagenstatus: Content): Map<String, Double> {
        with(Betriebszustand.from(anlagenstatus)) {
            val leistungsaufnahme = anlagenstatus.readValue("Leistungsaufnahme", KW_SUFFIX).asFuzzyWatt()
            return mapOf(
                "heizung" to heizung(leistungsaufnahme),
                "warmwasser" to warmwasser(leistungsaufnahme),
                "gesamt" to leistungsaufnahme
            )
        }
    }

    enum class Betriebszustand {
        HEIZEN { // Heizung
            override fun heizung(leistungsaufnahme: Double) = leistungsaufnahme
            override fun warmwasser(leistungsaufnahme: Double) = 0.0
        },
        WW { // Warmwasser
            override fun heizung(leistungsaufnahme: Double) = 0.0
            override fun warmwasser(leistungsaufnahme: Double) = leistungsaufnahme
        },
        ABT, // Abtauung
        UNDEFINED;

        open fun heizung(leistungsaufnahme: Double) = leistungsaufnahme / 2
        open fun warmwasser(leistungsaufnahme: Double) = leistungsaufnahme / 2

        companion object {
            fun from(anlagenstatus: Content) = Betriebszustand.entries.find {
                it.name == anlagenstatus.readValue("Betriebszustand").uppercase()
            } ?: UNDEFINED
        }
    }

    private fun extractLeistungsaufnahmeGesamt(leistungsaufnahme: Content) =
        mapOf(
            "heizung" to leistungsaufnahme.readValue("Heizung", KWH_SUFFIX).toDouble(),
            "warmwasser" to leistungsaufnahme.readValue("Warmwasser", KWH_SUFFIX).toDouble(),
            "gesamt" to leistungsaufnahme.readValue("Gesamt", KWH_SUFFIX).toDouble()
        )
}