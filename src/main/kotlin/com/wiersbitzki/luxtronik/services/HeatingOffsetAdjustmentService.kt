package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.AdjustmentService
import com.wiersbitzki.luxtronik.Forecast
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.tryGetString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import java.math.BigDecimal
import java.math.BigDecimal.valueOf
import java.math.RoundingMode

private const val AUSSENTEMPERATUR = "Außentemperatur"
private const val AUSSENTEMPERATUR_REGISTER = 10108
private const val MODUS_HEIZEN = "Modus Heizen"
private const val MODUS_HEIZEN_REGISTER = 10000
private const val OFFSET_HEIZEN = "Offset Heizen"
private const val OFFSET_HEIZEN_REGISTER = 10002

@Single
class HeatingOffsetAdjustmentService(
    config: ApplicationConfig,
    private val longitude: String = config.tryGetString("ktor.location.longitude")!!,
    private val latitude: String = config.tryGetString("ktor.location.latitude")!!,
    private val timezone: String = config.tryGetString("ktor.location.timezone")!!,
    private val singleRegisterModbusService: SingleRegisterModbusService
) : AdjustmentService {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
        defaultRequest {
            url("https://api.open-meteo.com/v1/forecast")
            contentType(ContentType.Application.Json)
        }
    }
    private val localTemperature: BigDecimal
        get() = singleRegisterModbusService.readInput(AUSSENTEMPERATUR, AUSSENTEMPERATUR_REGISTER)
            .toBigDecimal()
            .also { logger.debug("###### local temperature: {}", it.format()) }

    override suspend fun adjust() {
        val offsetHeizenSoll = localTemperature
            .minus(forecastTemperature())
            .multiply(valueOf(2))
            .setScale(-1, RoundingMode.HALF_DOWN)
            .divide(valueOf(2))
            .toInt()
            .also { logger.info("###### Offset Heizen Soll: {}", it.format()) }
        if (offsetHeizenSoll == 0) {
            reset()
            return
        }
        val modusHeizenIst = singleRegisterModbusService.read(MODUS_HEIZEN, MODUS_HEIZEN_REGISTER)
            .also { logger.info("###### Modus Heizen: {}", it) }
        val modusHeizenSoll = 2
        if (modusHeizenIst != modusHeizenSoll) {
            singleRegisterModbusService.write(MODUS_HEIZEN, MODUS_HEIZEN_REGISTER, modusHeizenSoll)
        }
        val offsetHeizenIst = status()
        if (offsetHeizenSoll != offsetHeizenIst) {
            singleRegisterModbusService.write(OFFSET_HEIZEN, OFFSET_HEIZEN_REGISTER, offsetHeizenSoll)
        }
    }

    override suspend fun status() = singleRegisterModbusService.read(OFFSET_HEIZEN, OFFSET_HEIZEN_REGISTER)
        .also { logger.info("###### Offset Heizen Ist: {}", it.format()) }

    override suspend fun reset() {
        singleRegisterModbusService.write(MODUS_HEIZEN, MODUS_HEIZEN_REGISTER, 0)
        singleRegisterModbusService.write(OFFSET_HEIZEN, OFFSET_HEIZEN_REGISTER, 0)
    }

    private suspend fun forecastTemperature() =
        client.get() {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("current", "temperature_2m")
            parameter("timezone", timezone)
        }.body<Forecast>()
            .current
            .temperature2m
            .toBigDecimal()
            .multiply(BigDecimal.TEN)
            .setScale(0, RoundingMode.HALF_UP)
            .also { logger.debug("###### forecast temperature: {}", it.format()) }

    private fun Int.format() = "%03.1f".format(this / 10.0)
    private fun BigDecimal.format() = "%03.1f".format(this.toDouble() / 10.0)
}
