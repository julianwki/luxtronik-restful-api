package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Content
import com.wiersbitzki.luxtronik.Navigation
import io.ktor.client.plugins.websocket.*
import io.ktor.util.logging.*
import io.ktor.websocket.*
import org.koin.core.annotation.Singleton
import java.math.RoundingMode

private const val KWH_SUFFIX = "kWh"

@Singleton
class CoefficientOfPerformanceService(
    private val websocketService: WebsocketService
) : DataService {

    internal val logger = KtorSimpleLogger(CoefficientOfPerformanceService::class.java.simpleName)

    override fun data(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        websocketService.execute {
            receiveDeserialized<Navigation>()
                .let { navigation ->
                    send("GET;${navigation.waermemengeId}")
                    val waermemenge = receiveDeserialized<Content>()
                    send("GET;${navigation.leistungsaufnahmeId}")
                    val leistungsaufnahme = receiveDeserialized<Content>()
                    data.put("heizung", (waermemenge.heizung / leistungsaufnahme.heizung).round())
                    data.put("warmwasser", (waermemenge.warmwasser / leistungsaufnahme.warmwasser).round())
                    data.put("gesamt", (waermemenge.gesamt / leistungsaufnahme.gesamt).round())
                }
        }
        return data.also { logger.info("$it") }
    }

    private val Navigation.waermemengeId
        get() = readNavigationId("Informationen", "Energiemonitor", "Wärmemenge")
            .also { logger.debug("Navigation: {}", this) }
    private val Navigation.leistungsaufnahmeId
        get() = readNavigationId("Informationen", "Energiemonitor", "Leistungsaufnahme")
            .also { logger.debug("Navigation: {}", this) }

    private val Content.heizung
        get() = readValue("Heizung", KWH_SUFFIX)
            .also { logger.info("Heizung: {}", it) }
            .toDouble()
            .also { logger.debug("Content: {}", this) }
    private val Content.warmwasser
        get() = readValue("Warmwasser", KWH_SUFFIX)
            .also { logger.info("Warmwasser: {}", it) }
            .toDouble()
            .also { logger.debug("Content: {}", this) }
    private val Content.gesamt
        get() = readValue("Gesamt", KWH_SUFFIX)
            .also { logger.info("Gesamt: {}", it) }
            .toDouble()
            .also { logger.debug("Content: {}", this) }

    private fun Double.round() = toBigDecimal()
        .setScale(2, RoundingMode.HALF_UP)
        .toDouble()
}