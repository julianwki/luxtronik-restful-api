package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Content
import com.wiersbitzki.luxtronik.DataService
import com.wiersbitzki.luxtronik.Navigation
import com.wiersbitzki.luxtronik.asFuzzyWatt
import com.wiersbitzki.luxtronik.receiveContent
import com.wiersbitzki.luxtronik.round
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
            val leistungsaufnahme = anlagenstatus.readValue("Leistungsaufnahme", KW_SUFFIX).asFuzzyWatt()
            val heizleistung = anlagenstatus.readValue("Heizleistung Ist", KW_SUFFIX).asFuzzyWatt()
            return mapOf(
                "heizung" to heizung(heizleistung, leistungsaufnahme),
                "warmwasser" to warmwasser(heizleistung, leistungsaufnahme),
                "gesamt" to Pair(heizleistung, leistungsaufnahme).cop()
            )
        }
    }

    enum class Betriebszustand {
        HEIZEN { // Heizung
            override fun heizung(heizleistung: Double, leistungsaufnahme: Double) =
                Pair(heizleistung, leistungsaufnahme).cop()
        },
        WW { // Warmwasser
            override fun warmwasser(heizleistung: Double, leistungsaufnahme: Double) =
                Pair(heizleistung, leistungsaufnahme).cop()
        },
        ABT, // Abtauung
        UNDEFINED;

        open fun heizung(heizleistung: Double, leistungsaufnahme: Double) = 0.0
        open fun warmwasser(heizleistung: Double, leistungsaufnahme: Double) = 0.0

        companion object {
            fun from(anlagenstatus: Content) = Betriebszustand.entries.find {
                it.name == anlagenstatus.readValue("Betriebszustand").uppercase()
            } ?: UNDEFINED
        }
    }

    private fun extractArbeitszahlGesamt(waermemenge: Content, leistungsaufnahme: Content) =
        mapOf(
            "heizung" to Pair(waermemenge.heizung, leistungsaufnahme.heizung).cop(),
            "warmwasser" to Pair(waermemenge.warmwasser, leistungsaufnahme.warmwasser).cop(),
            "gesamt" to Pair(waermemenge.gesamt, leistungsaufnahme.gesamt).cop()
        )

    private val Content.heizung
        get() = readValue("Heizung", KWH_SUFFIX).toDouble()
    private val Content.warmwasser
        get() = readValue("Warmwasser", KWH_SUFFIX).toDouble()
    private val Content.gesamt
        get() = readValue("Gesamt", KWH_SUFFIX).toDouble()
}

private fun Pair<Double, Double>.cop() = if (second != 0.0) (first / second).round() else 0.0
