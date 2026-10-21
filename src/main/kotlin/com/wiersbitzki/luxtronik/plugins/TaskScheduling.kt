package com.wiersbitzki.luxtronik.plugins

import com.wiersbitzki.luxtronik.services.HeatingOffsetAdjustmentService
import io.ktor.server.application.Application
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject
import java.time.Duration
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinDuration

private val now: LocalTime
    get() = LocalTime.now()

fun Application.configureScheduling() {
    val logger = KtorSimpleLogger(this::class.simpleName!!)
    val heatingOffsetAdjustmentService by inject<HeatingOffsetAdjustmentService>()
    launch(Dispatchers.Default) {
        delay(1.minutes)
        while (true) {
            try {
                heatingOffsetAdjustmentService.adjust()
            } catch (e: Exception) {
                logger.error("Scheduled task failed", e)
            }
            val nextRun = now.nextRun()
            logger.info("Heating offset adjustment will run again at {}", nextRun)
            delay(nextRun.delay())
        }
    }
}

private fun LocalTime.nextRun(minuteOfHour: Long = 15) =
    (minuteOfHour - (this.minute % minuteOfHour))
        .let { this.plusMinutes(it).truncatedTo(ChronoUnit.MINUTES) }

private fun LocalTime.delay() = Duration.between(now, this).toKotlinDuration()