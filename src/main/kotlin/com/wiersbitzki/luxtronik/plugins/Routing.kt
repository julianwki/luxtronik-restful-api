package com.wiersbitzki.luxtronik.plugins

import com.ucasoft.ktor.simpleCache.cacheOutput
import com.wiersbitzki.luxtronik.services.CoefficientOfPerformanceService
import com.wiersbitzki.luxtronik.services.PowerConsumptionService
import com.wiersbitzki.luxtronik.services.SingleRegisterModbusService
import com.wiersbitzki.luxtronik.services.TemperatureService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject
import kotlin.time.Duration.Companion.seconds

fun Application.configureRouting() {
    val powerConsumptionService by inject<PowerConsumptionService>()
    val coefficientOfPerformanceService by inject<CoefficientOfPerformanceService>()
    val temperatureService by inject<TemperatureService>()
    val singleRegisterModbusService by inject<SingleRegisterModbusService>()
    routing {
        route("/leistungsaufnahme") {
            cacheOutput(10.seconds) {
                get() {
                    call.respond(powerConsumptionService.data())
                }
            }
        }
        route("/arbeitszahl") {
            cacheOutput(10.seconds) {
                get() {
                    call.respond(coefficientOfPerformanceService.data())
                }
            }
        }
        route("/temperaturen") {
            cacheOutput(10.seconds) {
                get() {
                    call.respond(temperatureService.data())
                }
            }
        }
        route("/warmwasserExtra") {
            val function = "Extra Warmwasser"
            val register = 10071
            get("status") {
                try {
                    call.respond(singleRegisterModbusService.status(function, register))
                } catch (_: Exception) {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
            post("an") {
                try {
                    singleRegisterModbusService.activate(function, register)
                    call.respond(HttpStatusCode.OK)
                } catch (_: Exception) {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
            post("aus") {
                try {
                    singleRegisterModbusService.deactivate(function, register)
                    call.respond(HttpStatusCode.OK)
                } catch (_: Exception) {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
        }
    }
}
