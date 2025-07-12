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
        }.also { logger.info(it.toString()) }

    private val Navigation.temperaturenId
        get() = readNavigationId("Informationen", "Temperaturen")

    private val Content.vorlauf
        get() = readValue("Vorlauf", CELSIUS_SUFFIX).toDouble()
    private val Content.ruecklauf
        get() = readValue("Rücklauf", CELSIUS_SUFFIX).toDouble()
    private val Content.ruecklaufSoll
        get() = readValue("Rückl.-Soll", CELSIUS_SUFFIX).toDouble()
    private val Content.heissgas
        get() = readValue("Heissgas", CELSIUS_SUFFIX).toDouble()
    private val Content.aussentemperatur
        get() = readValue("Außentemperatur", CELSIUS_SUFFIX).toDouble()
    private val Content.mitteltemperatur
        get() = readValue("Mitteltemperatur", CELSIUS_SUFFIX).toDouble()
    private val Content.warmwasserIst
        get() = readValue("Warmwasser-Ist", CELSIUS_SUFFIX).toDouble()
    private val Content.warmwasserSoll
        get() = readValue("Warmwasser-Soll", CELSIUS_SUFFIX).toDouble()
    private val Content.solarkollektor
        get() = readValue("Solarkollektor", CELSIUS_SUFFIX).toDouble()
    private val Content.solarspeicher
        get() = readValue("Solarspeicher", CELSIUS_SUFFIX).toDouble()
}