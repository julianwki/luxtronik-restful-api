package com.wiersbitzki.luxtronik

import com.wiersbitzki.luxtronik.plugins.configureCaching
import com.wiersbitzki.luxtronik.plugins.configureCallLogging
import com.wiersbitzki.luxtronik.plugins.configureDependencyInjection
import com.wiersbitzki.luxtronik.plugins.configureEndpoints
import com.wiersbitzki.luxtronik.plugins.configurePrometheus
import com.wiersbitzki.luxtronik.plugins.configureScheduling
import com.wiersbitzki.luxtronik.plugins.configureSerialization
import io.ktor.server.application.Application
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.logging.Logger

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureDependencyInjection()
    configurePrometheus()
    configureSerialization()
    configureCallLogging(developmentMode)
    configureCaching()
    configureEndpoints()
    configureScheduling()
}

interface Logging {
    val logger: Logger
        get() = KtorSimpleLogger(this::class.simpleName!!)
}