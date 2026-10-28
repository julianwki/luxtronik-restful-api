package com.wiersbitzki.luxtronik.plugins

import com.wiersbitzki.luxtronik.services.HeatingOffsetAdjustmentService
import com.wiersbitzki.luxtronik.services.SingleRegisterModbusService
import com.wiersbitzki.luxtronik.services.WebsocketService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.dsl.module
import org.koin.ksp.generated.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDependencyInjection() {
    install(Koin) {
        slf4jLogger()
        modules(
            Services().module,
            module {
                single { WebsocketService.Config(environment.config) }
                single { SingleRegisterModbusService.Config(environment.config) }
                single { HeatingOffsetAdjustmentService.Config(environment.config) }
                single { PrometheusMeterRegistry(PrometheusConfig.DEFAULT) }
            }
        )
    }
}

@Module
@ComponentScan("com.wiersbitzki.luxtronik.services")
class Services
