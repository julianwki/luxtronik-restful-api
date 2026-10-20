package com.wiersbitzki.luxtronik.plugins

import com.ucasoft.ktor.simpleCache.cacheOutput
import com.wiersbitzki.luxtronik.services.CoefficientOfPerformanceService
import com.wiersbitzki.luxtronik.services.HeatingOffsetAdjustmentService
import com.wiersbitzki.luxtronik.services.PowerConsumptionService
import com.wiersbitzki.luxtronik.services.SingleRegisterModbusService
import com.wiersbitzki.luxtronik.services.TemperatureService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject
import kotlin.time.Duration.Companion.seconds

fun Application.configureEndpoints() {
    val powerConsumptionService by inject<PowerConsumptionService>()
    val coefficientOfPerformanceService by inject<CoefficientOfPerformanceService>()
    val temperatureService by inject<TemperatureService>()
    val singleRegisterModbusService by inject<SingleRegisterModbusService>()
    val heatingOffsetAdjustmentService by inject<HeatingOffsetAdjustmentService>()
    routing {
        route("/leistungsaufnahme") {
            cacheOutput(10.seconds) {
                get() {
                    try {
                        call.respond(powerConsumptionService.data())
                    } catch (_: Exception) {
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }
        route("/arbeitszahl") {
            cacheOutput(10.seconds) {
                get() {
                    try {
                        call.respond(coefficientOfPerformanceService.data())
                    } catch (_: Exception) {
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }
        route("/temperaturen") {
            cacheOutput(10.seconds) {
                get() {
                    try {
                        call.respond(temperatureService.data())
                    } catch (_: Exception) {
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }
        route("/warmwasserExtra") {
            val function = "Extra Warmwasser"
            val register = 10071
            get("status") {
                try {
                    singleRegisterModbusService.read(function, register)
                        .let { call.respondText(it.toString()) }
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
        route("/offsetHeizen") {
            get("status") {
                try {
                    heatingOffsetAdjustmentService.status()
                        .let { call.respondText(it.toString()) }
                } catch (_: Exception) {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
            post("reset") {
                try {
                    heatingOffsetAdjustmentService.reset()
                    call.respond(HttpStatusCode.OK)
                } catch (_: Exception) {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
        }
    }
}
