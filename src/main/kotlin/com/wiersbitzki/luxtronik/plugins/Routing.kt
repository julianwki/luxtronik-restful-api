package com.wiersbitzki.luxtronik.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/leistungsaufnahme") {
            call.respond(
                mapOf(
                    "heizung" to 48.9,
                    "warmwasser" to 57.4
                )
            )
        }
    }
}
