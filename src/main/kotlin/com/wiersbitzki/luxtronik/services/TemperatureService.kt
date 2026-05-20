package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.DataService
import com.wiersbitzki.luxtronik.Navigation
import io.ktor.client.plugins.websocket.receiveDeserialized
import org.koin.core.annotation.Single

private const val CELSIUS_SUFFIX = "°C"

@Single
class TemperatureService(
    private val websocketService: WebsocketService
) : DataService {

    override fun data(): Map<String, Any> =
        websocketService.retrieveData { result ->
            receiveDeserialized<Navigation>()
                .let {
                    val temperaturen = receiveContent(it.temperaturenId)
                    result["vorlauf"] = temperaturen.readDouble("Vorlauf", CELSIUS_SUFFIX)
                    result["ruecklauf"] = temperaturen.readDouble("Rücklauf", CELSIUS_SUFFIX)
                    result["ruecklaufSoll"] = temperaturen.readDouble("Rückl.-Soll", CELSIUS_SUFFIX)
                    result["heissgas"] = temperaturen.readDouble("Heissgas", CELSIUS_SUFFIX)
                    result["aussentemperatur"] = temperaturen.readDouble("Außentemperatur", CELSIUS_SUFFIX)
                    result["mitteltemperatur"] = temperaturen.readDouble("Mitteltemperatur", CELSIUS_SUFFIX)
                    result["warmwasserIst"] = temperaturen.readDouble("Warmwasser-Ist", CELSIUS_SUFFIX)
                    result["warmwasserSoll"] = temperaturen.readDouble("Warmwasser-Soll", CELSIUS_SUFFIX)
                    result["solarkollektor"] = temperaturen.readDouble("Solarkollektor", CELSIUS_SUFFIX)
                    result["solarspeicher"] = temperaturen.readDouble("Solarspeicher", CELSIUS_SUFFIX)
                }
        }.also { logger.info(it.toString()) }

    private val Navigation.temperaturenId
        get() = readNavigationId("Informationen", "Temperaturen")

}