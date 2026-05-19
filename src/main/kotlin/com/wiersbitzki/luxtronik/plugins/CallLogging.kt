package com.wiersbitzki.luxtronik.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import org.slf4j.event.Level


fun Application.configureCallLogging(logAllCalls: Boolean = false) {
    install(CallLogging) {
        level = Level.INFO
        filter {
            it.request.path().startsWith("/") && (logAllCalls || it.response.status() != HttpStatusCode.OK)
        }
        format {
            val coloredStatus = it.response.status()
                .let { statusCode ->
                    when {
                        statusCode == null -> "\u001B[33mUNKNOWN\u001B[0m"
                        statusCode.value < HttpStatusCode.MultipleChoices.value -> "\u001B[32m$statusCode\u001B[0m"
                        statusCode.value < HttpStatusCode.BadRequest.value -> "\u001B[33m$statusCode\u001B[0m"
                        else -> "\u001B[31m$statusCode\u001B[0m"
                    }
                }
            val coloredMethod = it.request.httpMethod.value.let { value -> "\u001B[36m$value\u001B[0m" }
            val queryParams = it.request.queryParameters.entries()
                .let { entries ->
                    if (entries.isEmpty()) "none"
                    else entries.joinToString(", ") { entry -> "${entry.key}=${entry.value}" }
                }
            """
            |
            |------------------------ Request Details ------------------------
            |Status: $coloredStatus
            |Method: $coloredMethod
            |Path: ${it.request.path()}
            |Query Parameters: $queryParams
            |Remote Host: ${it.request.origin.remoteHost}
            |User Agent: ${it.request.headers["User-Agent"]}
            |Duration: ${it.processingTimeMillis()}ms
            |------------------------------------------------------------------
            |
            """.trimMargin()
        }
    }
}
