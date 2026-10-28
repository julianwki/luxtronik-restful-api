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
    private val config: Config,
    private val singleRegisterModbusService: SingleRegisterModbusService
) : AdjustmentService {

    data class Config(
        val longitude: Double,
        val latitude: Double,
        val timezone: String,
        val active: Boolean,
    ) {
        constructor(config: ApplicationConfig) : this(
            longitude = config.property("service.heatingOffsetAdjustment.longitude").getString().toDouble(),
            latitude = config.property("service.heatingOffsetAdjustment.latitude").getString().toDouble(),
            timezone = config.property("service.heatingOffsetAdjustment.timezone").getString(),
            active = config.property("service.heatingOffsetAdjustment.active").getString().toBoolean(),
        )
    }

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
    private val aussentemperatur: BigDecimal
        get() = singleRegisterModbusService.readInput(AUSSENTEMPERATUR, AUSSENTEMPERATUR_REGISTER)
            .toBigDecimal()
            .also { logger.debug("$AUSSENTEMPERATUR: {}", it.format()) }

    override suspend fun adjust() {
        val forecastTemperature = forecastTemperature()
        val localTemperature = aussentemperatur
        val offsetHeizenSoll = localTemperature
            .minus(forecastTemperature)
            .multiply(valueOf(2))
            .setScale(-1, RoundingMode.DOWN)
            .divide(valueOf(2))
            .toInt()
        logger.info(
            "Forecast/Local {}°C/{}°C, calculated Offset is {}K",
            forecastTemperature.format(),
            localTemperature.format(),
            offsetHeizenSoll.format()
        )
        if (config.active.not()) {
            logger.warn("Adjustment deactivated, not writing values!")
            return
        }
        if (offsetHeizenSoll == 0) {
            reset()
            return
        }
        val modusHeizenIst = singleRegisterModbusService.read(MODUS_HEIZEN, MODUS_HEIZEN_REGISTER)
        val modusHeizenSoll = 2
        if (modusHeizenIst != modusHeizenSoll) {
            singleRegisterModbusService.write(MODUS_HEIZEN, MODUS_HEIZEN_REGISTER, modusHeizenSoll)
        }
        val offsetHeizenIst = status()
        if (offsetHeizenSoll != offsetHeizenIst) {
            singleRegisterModbusService.write(OFFSET_HEIZEN, OFFSET_HEIZEN_REGISTER, offsetHeizenSoll)
            logger.info("Change $OFFSET_HEIZEN to {}K", offsetHeizenSoll.format())
        } else {
            logger.info("$OFFSET_HEIZEN does not need to be changed")
        }
    }

    override suspend fun status() = singleRegisterModbusService.read(OFFSET_HEIZEN, OFFSET_HEIZEN_REGISTER)
        .also { logger.info("$OFFSET_HEIZEN is {}K", it.format()) }

    override suspend fun reset() {
        if (config.active.not()) {
            logger.warn("Adjustment deactivated, not writing values!")
            return
        }
        singleRegisterModbusService.write(MODUS_HEIZEN, MODUS_HEIZEN_REGISTER, 0)
        singleRegisterModbusService.write(OFFSET_HEIZEN, OFFSET_HEIZEN_REGISTER, 0)
        logger.info("Reset $OFFSET_HEIZEN to 0K")
    }

    private suspend fun forecastTemperature() =
        client.get {
            parameter("latitude", config.latitude)
            parameter("longitude", config.longitude)
            parameter("current", "temperature_2m")
            parameter("timezone", config.timezone)
        }.body<Forecast>()
            .also { logger.debug("Forecast: {}", Json.encodeToString(it)) }
            .current
            .temperature2m
            .toBigDecimal()
            .multiply(BigDecimal.TEN)
            .setScale(0, RoundingMode.HALF_UP)

    private fun Int.format() = "%03.1f".format(this / 10.0)
    private fun BigDecimal.format() = "%03.1f".format(this.toDouble() / 10.0)
}