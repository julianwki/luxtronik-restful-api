package com.wiersbitzki.luxtronik

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*

class ApplicationTest : ShouldSpec({
    context("application") {

        should("smoke test") {
            testApplication {
                application {
                    module()
                }
                client.get("/leistungsaufnahme").apply {
                    status shouldBe HttpStatusCode.OK
                }
            }
        }
    }


})