package com.wiersbitzki.luxtronik

import com.wiersbitzki.luxtronik.plugins.configureDependencyInjection
import com.wiersbitzki.luxtronik.plugins.configureRouting
import com.wiersbitzki.luxtronik.plugins.configureSerialization
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureDependencyInjection()
    configureSerialization()
    configureRouting()
}
