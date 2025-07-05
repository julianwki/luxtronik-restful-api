package com.wiersbitzki.luxtronik

import com.wiersbitzki.luxtronik.plugins.configureCaching
import com.wiersbitzki.luxtronik.plugins.configureCallLogging
import com.wiersbitzki.luxtronik.plugins.configureDependencyInjection
import com.wiersbitzki.luxtronik.plugins.configureRouting
import com.wiersbitzki.luxtronik.plugins.configureSerialization
import io.ktor.server.application.Application

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureDependencyInjection()
    configureSerialization()
    configureCallLogging()
    configureCaching()
    configureRouting()
}
