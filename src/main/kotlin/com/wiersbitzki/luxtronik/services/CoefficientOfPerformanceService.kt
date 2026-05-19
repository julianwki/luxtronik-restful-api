package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Content
import com.wiersbitzki.luxtronik.DataService
import com.wiersbitzki.luxtronik.Navigation
import com.wiersbitzki.luxtronik.receiveContent
import io.ktor.client.plugins.websocket.receiveDeserialized
import org.koin.core.annotation.Single

private const val KWH_SUFFIX = "kWh"
private const val KW_SUFFIX = "kW"

@Single
class CoefficientOfPerformanceService(
    private val websocketService: WebsocketService
) : DataService {

    override fun data(): Map<String, Any> =
        websocketService.retrieveData { result ->
            receiveDeserialized<Navigation>()
                .let {
                    val anlagenstatus = receiveContent(it.anlagenstatusId)
                    result["aktuell"] = extractArbeitszahlAktuell(anlagenstatus)
                    val waermemenge = receiveContent(it.waermemengeId)
                    val leistungsaufnahme = receiveContent(it.leistungsaufnahmeId)
                    result["gesamt"] = extractArbeitszahlGesamt(waermemenge, leistungsaufnahme)
                }
        }.also { logger.info(it.toString()) }

    private val Navigation.anlagenstatusId
        get() = readNavigationId("Informationen", "Anlagenstatus")
    private val Navigation.waermemengeId
        get() = readNavigationId("Informationen", "Energiemonitor", "Wärmemenge")
    private val Navigation.leistungsaufnahmeId
        get() = readNavigationId("Informationen", "Energiemonitor", "Leistungsaufnahme")

    private fun extractArbeitszahlAktuell(anlagenstatus: Content) =
        Betriebszustand.from(anlagenstatus).let {
            val leistungsaufnahme = anlagenstatus.readDouble("Leistungsaufnahme", KW_SUFFIX)
            val heizleistung = anlagenstatus.readDouble("Heizleistung Ist", KW_SUFFIX)
            mapOf(
                "heizung" to it.copHeizung(heizleistung, leistungsaufnahme),
                "warmwasser" to it.copWarmwasser(heizleistung, leistungsaufnahme),
                "gesamt" to it.copGesamt(heizleistung, leistungsaufnahme),
            )
        }

    private fun extractArbeitszahlGesamt(waermemenge: Content, leistungsaufnahme: Content) =
        Betriebszustand.UNDEFINED.let {
            mapOf(
                "heizung" to it.copHeizung(waermemenge.heizung, leistungsaufnahme.heizung),
                "warmwasser" to it.copWarmwasser(waermemenge.warmwasser, leistungsaufnahme.warmwasser),
                "gesamt" to it.copGesamt(waermemenge.gesamt, leistungsaufnahme.gesamt),
            )
        }

    private val Content.heizung
        get() = readDouble("Heizung", KWH_SUFFIX)
    private val Content.warmwasser
        get() = readDouble("Warmwasser", KWH_SUFFIX)
    private val Content.gesamt
        get() = readDouble("Gesamt", KWH_SUFFIX)
}