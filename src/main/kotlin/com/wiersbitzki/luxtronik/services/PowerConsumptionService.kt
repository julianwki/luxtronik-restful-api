package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Content
import com.wiersbitzki.luxtronik.DataService
import com.wiersbitzki.luxtronik.Navigation
import com.wiersbitzki.luxtronik.asFuzzyWatt
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

    private fun Content.extractLeistungsaufnahmeAktuell() =
        mapOf(
            "heizung" to betriebszustand.heizung(leistungsaufnahme),
            "warmwasser" to betriebszustand.warmwasser(leistungsaufnahme),
            "gesamt" to leistungsaufnahme
        )

    private val Content.betriebszustand
        get() = Betriebszustand.entries
            .find { it.name == readValue("Betriebszustand").uppercase() } ?: Betriebszustand.UNDEFINED
    private val Content.leistungsaufnahme
        get() = readValue("Leistungsaufnahme", KW_SUFFIX).asFuzzyWatt()

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
    }

    private fun Content.extractLeistungsaufnahmeGesamt() =
        mapOf(
            "heizung" to readValue("Heizung", KWH_SUFFIX).toDouble(),
            "warmwasser" to readValue("Warmwasser", KWH_SUFFIX).toDouble(),
            "gesamt" to readValue("Gesamt", KWH_SUFFIX).toDouble()
        )
}