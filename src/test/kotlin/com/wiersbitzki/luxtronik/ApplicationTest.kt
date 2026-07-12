package com.wiersbitzki.luxtronik

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeIn
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.testApplication

class ApplicationTest : ShouldSpec({
    context("application") {

        should("smoke test") {
            testApplication {
                environment {
                    config = ApplicationConfig("application.yaml")
                }
                client.get("/leistungsaufnahme").apply {
                    status shouldBeIn listOf(HttpStatusCode.OK, HttpStatusCode.InternalServerError)
                }
            }
        }
    }


})