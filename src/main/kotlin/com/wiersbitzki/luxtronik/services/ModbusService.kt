package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Logging
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.tryGetString

class ModbusService(
    private val function: String,
    config: ApplicationConfig,
    private val host: String = config.tryGetString("ktor.service.modbus.host")!!,
    private val port: String = config.tryGetString("ktor.service.modbus.port")!!
) : Logging {

    suspend fun activate(): Map<String, String> {
        logger.info("Activating $function with $host:$port")
        return mapOf(
            function to "an"
        )
    }

    suspend fun deactivate(): Map<String, String> {
        logger.info("Deactivating $function with $host:$port")
        return mapOf(
            function to "aus"
        )
    }

    suspend fun status(): Map<String, String> {
        logger.info("Status of $function with $host:$port")
        return mapOf(
            function to "status"
        )
    }
}