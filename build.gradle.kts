import io.ktor.plugin.features.DockerPortMapping
import io.ktor.plugin.features.DockerPortMappingProtocol

val appName = providers.gradleProperty("app_name").get()
val appGroup = providers.gradleProperty("app_group").get()
val appVersion = providers.gradleProperty("app_version").get()

val kotlinVersion = providers.gradleProperty("kotlin_version").get()
val javaVersion = providers.gradleProperty("java_version").get()
val ktorVersion = providers.gradleProperty("ktor_version").get()
val logbackVersion = providers.gradleProperty("logback_version").get()
val koinVersion = providers.gradleProperty("koin_version").get()
val koinKspVersion = providers.gradleProperty("koin_ksp_version").get()
val ktorSimpleCacheVersion = providers.gradleProperty("ktor_simple_cache_version").get()
val kotestVersion = providers.gradleProperty("kotest_version").get()
val j2modVersion = providers.gradleProperty("j2mod_version").get()

plugins {
    kotlin("jvm") version "2.3.21"
    id("io.ktor.plugin") version "3.3.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21"
    id("com.google.devtools.ksp") version "2.3.3"
}

sourceSets.main {
    kotlin.srcDirs("build/generated/ksp/main/kotlin")
}

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(javaVersion.toInt())
}

ktor {
    docker {
        jreVersion.set(JavaVersion.toVersion(javaVersion))
        localImageName.set(appName)
        imageTag.set(appVersion)
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
    implementation("io.ktor:ktor-server-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-server-core:${ktorVersion}")
    implementation("io.ktor:ktor-server-call-logging:${ktorVersion}")
    implementation("io.ktor:ktor-server-netty:${ktorVersion}")
    implementation("io.ktor:ktor-client-core:${ktorVersion}")
    implementation("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-client-cio:${ktorVersion}")
    implementation("io.ktor:ktor-client-websockets:${ktorVersion}")
    implementation("ch.qos.logback:logback-classic:${logbackVersion}")
    implementation("com.ucasoft.ktor:ktor-simple-cache:${ktorSimpleCacheVersion}")
    implementation("com.ucasoft.ktor:ktor-simple-memory-cache:${ktorSimpleCacheVersion}")
    // Koin
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
    implementation("io.insert-koin:koin-annotations:$koinKspVersion")
    ksp("io.insert-koin:koin-ksp-compiler:$koinKspVersion")
    // ModbusTCP
    implementation("com.ghgande:j2mod:$j2modVersion")
    // testing
    implementation("io.ktor:ktor-server-config-yaml")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
}
