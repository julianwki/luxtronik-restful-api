package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Content
import com.wiersbitzki.luxtronik.DataService
import com.wiersbitzki.luxtronik.Navigation
import com.wiersbitzki.luxtronik.receiveContent
import io.ktor.client.plugins.websocket.*
import org.koin.core.annotation.Single
import kotlin.random.Random

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

    private fun extractLeistungsaufnahmeAktuell(anlagenstatus: Content) =
        Betriebszustand.from(anlagenstatus).let {
            val leistungsaufnahme = anlagenstatus.readDouble("Leistungsaufnahme", KW_SUFFIX).asFuzzyWatt()
            mapOf(
                "heizung" to it.heizung(leistungsaufnahme),
                "warmwasser" to it.warmwasser(leistungsaufnahme),
                "gesamt" to leistungsaufnahme
            )
        }

    private fun extractLeistungsaufnahmeGesamt(leistungsaufnahme: Content) =
        mapOf(
            "heizung" to leistungsaufnahme.readDouble("Heizung", KWH_SUFFIX),
            "warmwasser" to leistungsaufnahme.readDouble("Warmwasser", KWH_SUFFIX),
            "gesamt" to leistungsaufnahme.readDouble("Gesamt", KWH_SUFFIX)
        )

    private fun Double.asFuzzyWatt() = if (this > 0) this * 1000 + Random.nextInt(-5, 5) else 0.0
}