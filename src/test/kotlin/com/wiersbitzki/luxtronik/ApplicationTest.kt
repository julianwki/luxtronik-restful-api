package com.wiersbitzki.luxtronik

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
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
                client.get("/metrics").apply {
                    status shouldBe HttpStatusCode.OK
                }
            }
        }
    }

})