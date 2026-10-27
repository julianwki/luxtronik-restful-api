package com.wiersbitzki.luxtronik.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.koin.ktor.ext.inject

fun Application.configurePrometheus() {
    val prometheusMeterRegistry by inject<PrometheusMeterRegistry>()
    install(MicrometerMetrics) {
        registry = prometheusMeterRegistry
    }
}