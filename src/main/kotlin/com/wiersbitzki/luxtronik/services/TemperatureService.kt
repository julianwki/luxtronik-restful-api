package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Content
import com.wiersbitzki.luxtronik.Navigation
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.websocket.send
import org.koin.core.annotation.Singleton

private const val CELSIUS_SUFFIX = "°C"

@Singleton
class TemperatureService(
    private val websocketService: WebsocketService
) : DataService {

    override fun data(): Map<String, Any> =
        websocketService.retrieveData { result ->
            receiveDeserialized<Navigation>()
                .let { navigation ->
                    send("GET;${navigation.temperaturenId}")
                    val temperaturen = receiveDeserialized<Content>()
                    result.put("vorlauf", temperaturen.vorlauf)
                    result.put("ruecklauf", temperaturen.ruecklauf)
                    result.put("ruecklaufSoll", temperaturen.ruecklaufSoll)
                    result.put("heissgas", temperaturen.heissgas)
                    result.put("aussentemperatur", temperaturen.aussentemperatur)
                    result.put("mitteltemperatur", temperaturen.mitteltemperatur)
                    result.put("warmwasserIst", temperaturen.warmwasserIst)
                    result.put("warmwasserSoll", temperaturen.warmwasserSoll)
                    result.put("solarkollektor", temperaturen.solarkollektor)
                    result.put("solarspeicher", temperaturen.solarspeicher)
                }
        }.also { logger.info("$it") }

    private val Navigation.temperaturenId
        get() = readNavigationId("Informationen", "Temperaturen")
            .also { logger.debug("Navigation: {}", this) }

    private val Content.vorlauf
        get() = "Vorlauf".let { name ->
            readValue(name, CELSIUS_SUFFIX)
                .also { logger.info("{}: {}", name, it) }
                .toDouble()
                .also { logger.debug("Content: {}", this) }
        }
    private val Content.ruecklauf
        get() = "Rücklauf".let { name ->
            readValue(name, CELSIUS_SUFFIX)
                .also { logger.info("{}: {}", name, it) }
                .toDouble()
                .also { logger.debug("Content: {}", this) }
        }
    private val Content.ruecklaufSoll
        get() = "Rückl.-Soll".let { name ->
            readValue(name, CELSIUS_SUFFIX)
                .also { logger.info("{}: {}", name, it) }
                .toDouble()
                .also { logger.debug("Content: {}", this) }
        }
    private val Content.heissgas
        get() = "Heissgas".let { name ->
            readValue(name, CELSIUS_SUFFIX)
                .also { logger.info("{}: {}", name, it) }
                .toDouble()
                .also { logger.debug("Content: {}", this) }
        }
    private val Content.aussentemperatur
        get() = "Außentemperatur".let { name ->
            readValue(name, CELSIUS_SUFFIX)
                .also { logger.info("{}: {}", name, it) }
                .toDouble()
                .also { logger.debug("Content: {}", this) }
        }
    private val Content.mitteltemperatur
        get() = "Mitteltemperatur".let { name ->
            readValue(name, CELSIUS_SUFFIX)
                .also { logger.info("{}: {}", name, it) }
                .toDouble()
                .also { logger.debug("Content: {}", this) }
        }
    private val Content.warmwasserIst
        get() = "Warmwasser-Ist".let { name ->
            readValue(name, CELSIUS_SUFFIX)
                .also { logger.info("{}: {}", name, it) }
                .toDouble()
                .also { logger.debug("Content: {}", this) }
        }
    private val Content.warmwasserSoll
        get() = "Warmwasser-Soll".let { name ->
            readValue(name, CELSIUS_SUFFIX)
                .also { logger.info("{}: {}", name, it) }
                .toDouble()
                .also { logger.debug("Content: {}", this) }
        }
    private val Content.solarkollektor
        get() = "Solarkollektor".let { name ->
            readValue(name, CELSIUS_SUFFIX)
                .also { logger.info("{}: {}", name, it) }
                .toDouble()
                .also { logger.debug("Content: {}", this) }
        }
    private val Content.solarspeicher
        get() = "Solarspeicher".let { name ->
            readValue(name, CELSIUS_SUFFIX)
                .also { logger.info("{}: {}", name, it) }
                .toDouble()
                .also { logger.debug("Content: {}", this) }
        }
}