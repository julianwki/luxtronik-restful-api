package com.wiersbitzki.luxtronik.plugins

import com.ucasoft.ktor.simpleCache.SimpleCache
import com.ucasoft.ktor.simpleMemoryCache.memoryCache
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlin.time.Duration.Companion.minutes

fun Application.configureCaching() {
    install(SimpleCache) {
        memoryCache {
            invalidateAt = 1.minutes
        }
    }
}
