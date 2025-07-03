import io.ktor.plugin.features.*

val kotlin_version: String by project
val ktor_version: String by project
val logback_version: String by project
val koin_version: String by project
val ksp_version: String by project
val koin_ksp_version: String by project

plugins {
    kotlin("jvm") version "2.1.21"
    id("io.ktor.plugin") version "3.1.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"
    id("com.google.devtools.ksp") version "2.1.21-2.0.1"
}

sourceSets.main {
    kotlin.srcDirs("build/generated/ksp/main/kotlin")
}

group = "com.wiersbitzki.luxtronik"
version = "0.0.4"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

ktor {
    docker {
        jreVersion.set(JavaVersion.VERSION_21)
        localImageName.set("luxtronic-restful-api")
        imageTag.set(version.toString())
        portMappings.set(
            listOf(
                DockerPortMapping(
                    8080, 8080,
                    DockerPortMappingProtocol.TCP
                )
            )
        )
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core:${ktor_version}")
    implementation("io.ktor:ktor-server-content-negotiation:${ktor_version}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktor_version}")
    implementation("io.ktor:ktor-serialization-kotlinx-xml:${ktor_version}")
    implementation("io.ktor:ktor-server-netty:${ktor_version}")
    implementation("io.ktor:ktor-client-websockets:${ktor_version}")
    implementation("io.ktor:ktor-client-cio:${ktor_version}")
    implementation("ch.qos.logback:logback-classic:${logback_version}")
    // Koin
    implementation("io.insert-koin:koin-ktor:$koin_version")
    implementation("io.insert-koin:koin-logger-slf4j:$koin_version")
    implementation("io.insert-koin:koin-annotations:$koin_ksp_version")
    ksp("io.insert-koin:koin-ksp-compiler:$koin_ksp_version")
    // testing
    implementation("io.ktor:ktor-server-config-yaml")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
