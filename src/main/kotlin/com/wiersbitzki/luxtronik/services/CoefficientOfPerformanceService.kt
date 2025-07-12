package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Content
import com.wiersbitzki.luxtronik.Navigation
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.websocket.send
import org.koin.core.annotation.Singleton
import java.math.RoundingMode

private const val KWH_SUFFIX = "kWh"

@Singleton
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
                    result.put("heizung", cop(waermemenge.heizung, leistungsaufnahme.heizung))
                    result.put("warmwasser", cop(waermemenge.warmwasser, leistungsaufnahme.warmwasser))
                    result.put("gesamt", cop(waermemenge.gesamt, leistungsaufnahme.gesamt))
                }
        }.also { logger.info("$it") }

    private fun cop(waermemenge: Double, leistungsaufnahme: Double) =
        (waermemenge / leistungsaufnahme).round()

    private val Navigation.waermemengeId
        get() = readNavigationId("Informationen", "Energiemonitor", "Wärmemenge")
            .also { logger.debug("Navigation: {}", this) }
    private val Navigation.leistungsaufnahmeId
        get() = readNavigationId("Informationen", "Energiemonitor", "Leistungsaufnahme")
            .also { logger.debug("Navigation: {}", this) }

    private val Content.heizung
        get() = "Heizung".let { name ->
            readValue(name, KWH_SUFFIX)
                .also { logger.info("{}: {}", name, it) }
                .toDouble()
                .also { logger.debug("Content: {}", this) }
        }
    private val Content.warmwasser
        get() = "Warmwasser".let { name ->
            readValue("Warmwasser", KWH_SUFFIX)
                .also { logger.info("{}: {}", name, it) }
                .toDouble()
                .also { logger.debug("Content: {}", this) }
        }
    private val Content.gesamt
        get() = "Gesamt".let { name ->
            readValue("Gesamt", KWH_SUFFIX)
                .also { logger.info("{}: {}", name, it) }
                .toDouble()
                .also { logger.debug("Content: {}", this) }
        }
    private fun Double.round() = toBigDecimal()
        .setScale(2, RoundingMode.HALF_UP)
        .toDouble()
}