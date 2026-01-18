package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Content
import com.wiersbitzki.luxtronik.DataService
import com.wiersbitzki.luxtronik.Navigation
import com.wiersbitzki.luxtronik.round
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.websocket.send
import org.koin.core.annotation.Single

private const val KWH_SUFFIX = "kWh"

@Single
class CoefficientOfPerformanceService(
    private val websocketService: WebsocketService
) : DataService {

    override fun data(): Map<String, Any> =
        websocketService.retrieveData { result ->
            receiveDeserialized<Navigation>()
                .let { navigation ->
                    send("GET;${navigation.waermemengeId}")
                    val waermemenge = receiveDeserialized<Content>()
                    send("GET;${navigation.leistungsaufnahmeId}")
                    val leistungsaufnahme = receiveDeserialized<Content>()
                    Pair(waermemenge, leistungsaufnahme).let {
                        result.put("heizung", it.heizung)
                        result.put("warmwasser", it.warmwasser)
                        result.put("gesamt", it.gesamt)
                    }
                }
        }.also { logger.info(it.toString()) }

    private val Navigation.waermemengeId
        get() = readNavigationId("Informationen", "Energiemonitor", "Wärmemenge")
    private val Navigation.leistungsaufnahmeId
        get() = readNavigationId("Informationen", "Energiemonitor", "Leistungsaufnahme")

    private val Pair<Content, Content>.heizung: Double
        get() = (first.heizung / second.heizung).round()
    private val Pair<Content, Content>.warmwasser: Double
        get() = (first.warmwasser / second.warmwasser).round()
    private val Pair<Content, Content>.gesamt: Double
        get() = (first.gesamt / second.gesamt).round()

    private val Content.heizung
        get() = readValue("Heizung", KWH_SUFFIX).toDouble()
    private val Content.warmwasser
        get() = readValue("Warmwasser", KWH_SUFFIX).toDouble()
    private val Content.gesamt
        get() = readValue("Gesamt", KWH_SUFFIX).toDouble()
}