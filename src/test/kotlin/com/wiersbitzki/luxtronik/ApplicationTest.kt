package com.wiersbitzki.luxtronik

import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication

@Ignored("currently broken")
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