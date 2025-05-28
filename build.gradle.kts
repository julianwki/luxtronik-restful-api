import io.ktor.plugin.features.*

val kotlinVersion: String by project
val ktorVersion: String by project
val logbackVersion: String by project
val koinVersion: String by project
val kspVersion: String by project
val koinKspVersion: String by project

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
version = "0.0.1"

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
    implementation("io.ktor:ktor-server-core-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-server-netty-jvm:${ktorVersion}")
    implementation("ch.qos.logback:logback-classic:${logbackVersion}")
    // Koin
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
    implementation("io.insert-koin:koin-annotations:$koinKspVersion")
    ksp("io.insert-koin:koin-ksp-compiler:$koinKspVersion")
    // testing
    implementation("io.ktor:ktor-server-config-yaml")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}
