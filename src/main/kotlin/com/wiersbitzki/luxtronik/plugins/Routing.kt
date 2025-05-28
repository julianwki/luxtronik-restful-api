package com.wiersbitzki.luxtronik.plugins

import com.wiersbitzki.luxtronik.services.LeistungsaufnahmeService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val service by inject<LeistungsaufnahmeService>()
    routing {
        route("/leistungsaufnahme") {
            get() {
                call.respond(service.data())
            }
        }
    }
}
