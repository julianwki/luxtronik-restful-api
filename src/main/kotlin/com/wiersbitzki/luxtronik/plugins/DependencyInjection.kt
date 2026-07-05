package com.wiersbitzki.luxtronik.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.config.ApplicationConfig
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ksp.generated.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDependencyInjection() {
    install(Koin) {
        slf4jLogger()
        modules(
            Services().module,
            module { single { environment.config } bind ApplicationConfig::class }
        )
    }
}

@Module
@ComponentScan("com.wiersbitzki.luxtronik.services")
class Services
