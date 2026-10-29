package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.AdjustmentService
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import java.math.BigDecimal
import java.math.RoundingMode

private const val AUSSENTEMPERATUR = "Außentemperatur"
private const val AUSSENTEMPERATUR_REGISTER = 10108
private const val MODUS_HEIZEN = "Modus Heizen"
private const val MODUS_HEIZEN_REGISTER = 10000
private const val MODUS_HEIZEN_AUS = 0
private const val MODUS_HEIZEN_OFFSET = 2
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
        val temperature: Temperature,
    ) {
        constructor(config: ApplicationConfig) : this(
            longitude = config.property("service.heatingOffsetAdjustment.longitude").getString().toDouble(),
            latitude = config.property("service.heatingOffsetAdjustment.latitude").getString().toDouble(),
            timezone = config.property("service.heatingOffsetAdjustment.timezone").getString(),
            active = config.property("service.heatingOffsetAdjustment.active").getString().toBoolean(),
            temperature = Temperature from config.property("service.heatingOffsetAdjustment.temperature").getString()
        )
    }

    enum class Temperature(val value: String) {
        ALL("all"),
        TEMPERATURE_2M("temperature_2m") {
            override fun from(current: Current) = current.temperature2m.toScaledBigDecimal()
        },
        APPARENT_TEMPERATURE("apparent_temperature") {
            override fun from(current: Current) = current.apparentTemperature.toScaledBigDecimal()
        };

        companion object {
            infix fun from(value: String): Temperature = entries.first { it.value == value }
        }

        open fun from(current: Current): BigDecimal =
            with(current) { listOf(temperature2m, apparentTemperature).average() }.toScaledBigDecimal()

        internal fun Double.toScaledBigDecimal() = this.toBigDecimal().setScale(1, RoundingMode.HALF_UP)
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
            .divide(BigDecimal.TEN)
            .also { logger.debug("$AUSSENTEMPERATUR: {}°C,", it.format()) }

    override suspend fun adjust() {
        val forecastTemperature = forecastTemperature()
        val localTemperature = aussentemperatur
        val offsetHeizenSoll = localTemperature.minus(forecastTemperature).roundHalfDecimal()
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
        if (offsetHeizenSoll == BigDecimal.ZERO) {
            reset()
            return
        }
        if (singleRegisterModbusService.read(MODUS_HEIZEN, MODUS_HEIZEN_REGISTER) != MODUS_HEIZEN_OFFSET) {
            singleRegisterModbusService.write(MODUS_HEIZEN, MODUS_HEIZEN_REGISTER, MODUS_HEIZEN_OFFSET)
        }
        val offsetHeizenIst = status()
        if (offsetHeizenSoll != offsetHeizenIst) {
            singleRegisterModbusService.write(
                OFFSET_HEIZEN, OFFSET_HEIZEN_REGISTER,
                offsetHeizenSoll.multiply(BigDecimal.TEN).toInt()
            )
            logger.info("Change $OFFSET_HEIZEN to {}K", offsetHeizenSoll.format())
        } else {
            logger.info("$OFFSET_HEIZEN does not need to be changed")
        }
    }

    override suspend fun status(): BigDecimal = singleRegisterModbusService.read(OFFSET_HEIZEN, OFFSET_HEIZEN_REGISTER)
        .toBigDecimal().divide(BigDecimal.TEN)
        .also { logger.info("$OFFSET_HEIZEN is {}K", it.format()) }

    override suspend fun reset() {
        if (config.active.not()) {
            logger.warn("Adjustment deactivated, not writing values!")
            return
        }
        singleRegisterModbusService.write(MODUS_HEIZEN, MODUS_HEIZEN_REGISTER, MODUS_HEIZEN_AUS)
        singleRegisterModbusService.write(OFFSET_HEIZEN, OFFSET_HEIZEN_REGISTER, 0)
        logger.info("Reset $OFFSET_HEIZEN to 0K")
    }

    private suspend fun forecastTemperature() =
        with(config) {
            client.get {
                parameter("latitude", latitude)
                parameter("longitude", longitude)
                parameter("current", "temperature_2m,apparent_temperature")
                parameter("timezone", timezone)
            }.body<Forecast>()
                .also { logger.debug("Forecast: {}", Json.encodeToString(it)) }
                .let { temperature.from(it.current) }
        }

    private fun BigDecimal.format() = "%03.1f".format(this)
    private fun BigDecimal.roundHalfDecimal() = this
        .multiply(BigDecimal.TWO)
        .setScale(0, RoundingMode.DOWN)
        .divide(BigDecimal.TWO)


    @Serializable
    data class Forecast(
        val latitude: Double,
        val longitude: Double,
        @SerialName("generationtime_ms")
        val generationtimeMs: Double,
        @SerialName("utc_offset_seconds")
        val utcOffsetSeconds: Int,
        val timezone: String,
        @SerialName("timezone_abbreviation")
        val timezoneAbbreviation: String,
        val elevation: Double,
        @SerialName("current_units")
        val currentUnits: CurrentUnits,
        val current: Current,
    )

    @Serializable
    data class CurrentUnits(
        val time: String,
        val interval: String,
        @SerialName("temperature_2m")
        val temperature2m: String,
        @SerialName("apparent_temperature")
        val apparentTemperature: String,
    )

    @Serializable
    data class Current(
        val time: String,
        val interval: Int,
        @SerialName("temperature_2m")
        val temperature2m: Double,
        @SerialName("apparent_temperature")
        val apparentTemperature: Double,
    )
}