package com.wiersbitzki.luxtronik.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.plugins.origin
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import org.slf4j.event.Level


fun Application.configureCallLogging(logAllCalls: Boolean = false) {
    install(CallLogging) {
        level = Level.INFO
        filter { call ->
            with(call) {
                request.path().startsWith("/") && (logAllCalls || response.status() != HttpStatusCode.OK)
            }
        }
        format { call ->
            with(call) {
                val coloredStatus = call.response.status().let {
                    when {
                        it == null -> "\u001B[33mUNKNOWN\u001B[0m"
                        it.value < HttpStatusCode.MultipleChoices.value -> "\u001B[32m$it\u001B[0m"
                        it.value < HttpStatusCode.BadRequest.value -> "\u001B[33m$it\u001B[0m"
                        else -> "\u001B[31m$it\u001B[0m"
                    }
                }
                val coloredMethod = request.httpMethod.value.let { "\u001B[36m$it\u001B[0m" }
                val queryParams = request.queryParameters.entries().let {
                    if (it.isEmpty()) "none"
                    else it.joinToString(", ") { "${it.key}=${it.value}" }
                }
                """
                |
                |------------------------ Request Details ------------------------
                |Status: $coloredStatus
                |Method: $coloredMethod
                |Path: ${request.path()}
                |Query Parameters: $queryParams
                |Remote Host: ${request.origin.remoteHost}
                |User Agent: ${request.headers["User-Agent"]}
                |Duration: ${processingTimeMillis()}ms
                |------------------------------------------------------------------
                |
                """.trimMargin()
            }
        }
    }
}
