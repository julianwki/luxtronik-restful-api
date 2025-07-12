package com.wiersbitzki.luxtronik.plugins

import com.ucasoft.ktor.simpleCache.cacheOutput
import com.wiersbitzki.luxtronik.services.CoefficientOfPerformanceService
import com.wiersbitzki.luxtronik.services.PowerConsumptionService
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject
import kotlin.time.Duration.Companion.seconds

fun Application.configureRouting() {
    val powerConsumptionService by inject<PowerConsumptionService>()
    val coefficientOfPerformanceService by inject<CoefficientOfPerformanceService>()
    routing {
        route("/leistungsaufnahme") {
            cacheOutput(10.seconds) {
                get() {
                    call.respond(powerConsumptionService.data())
                }
            }
        }
        route("/jahreszahl") {
            cacheOutput(10.seconds) {
                get() {
                    call.respond(coefficientOfPerformanceService.data())
                }
            }
        }
    }
}