package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Content
import com.wiersbitzki.luxtronik.DataService
import com.wiersbitzki.luxtronik.Navigation
import com.wiersbitzki.luxtronik.cop
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

    private fun extractArbeitszahlAktuell(anlagenstatus: Content): Map<String, Double> {
        with(Betriebszustand.from(anlagenstatus)) {
            val leistungsaufnahme = anlagenstatus.readDouble("Leistungsaufnahme", KW_SUFFIX)
            val heizleistung = anlagenstatus.readDouble("Heizleistung Ist", KW_SUFFIX)
            return mapOf(
                "heizung" to copHeizung(heizleistung, leistungsaufnahme),
                "warmwasser" to copWarmwasser(heizleistung, leistungsaufnahme),
                "gesamt" to Pair(heizleistung, leistungsaufnahme).cop()
            )
        }
    }

    private fun extractArbeitszahlGesamt(waermemenge: Content, leistungsaufnahme: Content) =
        mapOf(
            "heizung" to Pair(waermemenge.heizung, leistungsaufnahme.heizung).cop(),
            "warmwasser" to Pair(waermemenge.warmwasser, leistungsaufnahme.warmwasser).cop(),
            "gesamt" to Pair(waermemenge.gesamt, leistungsaufnahme.gesamt).cop()
        )

    private val Content.heizung
        get() = readDouble("Heizung", KWH_SUFFIX)
    private val Content.warmwasser
        get() = readDouble("Warmwasser", KWH_SUFFIX)
    private val Content.gesamt
        get() = readDouble("Gesamt", KWH_SUFFIX)
}