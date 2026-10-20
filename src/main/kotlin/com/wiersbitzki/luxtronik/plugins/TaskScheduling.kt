package com.wiersbitzki.luxtronik.plugins

import com.wiersbitzki.luxtronik.services.HeatingOffsetAdjustmentService
import io.ktor.server.application.Application
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject
import kotlin.time.Duration.Companion.minutes

fun Application.configureScheduling() {
    val logger = KtorSimpleLogger(this::class.simpleName!!)
    val heatingOffsetAdjustmentService by inject<HeatingOffsetAdjustmentService>()
    launch(Dispatchers.Default) {
        while (true) {
            try {
                heatingOffsetAdjustmentService.adjust()
            } catch (e: Exception) {
                logger.error("Scheduled task failed", e)
            }
            delay(15.minutes)
        }
    }
}