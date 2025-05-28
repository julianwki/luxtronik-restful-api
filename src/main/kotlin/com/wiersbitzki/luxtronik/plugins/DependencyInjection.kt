package com.wiersbitzki.luxtronik.plugins

import io.ktor.server.application.*
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.logger.slf4jLogger
import org.koin.ksp.generated.module
import org.koin.ktor.plugin.Koin

fun Application.configureDependencyInjection() {
    install(Koin) {
        slf4jLogger()
        modules(Services().module)
    }
}

@Module
@ComponentScan("com.wiersbitzki.luxtronik.services")
class Services
