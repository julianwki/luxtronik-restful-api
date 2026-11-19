# 2. Use Ktor with Koin for the HTTP server

- **Status:** Accepted
- **Date:** 2025-05-28

## Context

The service needs an HTTP server, an HTTP client (Open-Meteo), a WebSocket client (Luxtronik), JSON serialization,
and a way to wire services together. It is a small, single-purpose service that runs in a container on home
hardware.

## Decision

- Use **Kotlin** on the JVM (Java 21) and **Ktor** with the **Netty** engine for the server and **CIO** for clients.
- Use **kotlinx.serialization** for JSON.
- Use **Koin** for dependency injection, with **annotations and KSP** (`@Single`, `@ComponentScan`) to generate the
  module at compile time. Configuration objects are registered by hand in `DependencyInjection.kt`, because they are
  built from Ktor's `ApplicationConfig`.
- Each cross-cutting concern lives in its own `configureXxx()` function in the `plugins` package.

## Consequences

- Small footprint and fast startup compared with larger frameworks.
- Server, clients and WebSockets come from one library family and share coroutines.
- KSP adds a code-generation step (`build/generated/ksp`) to the build.
- Services are found by package scan of `com.wiersbitzki.luxtronik.services`. A service outside that package is not
  picked up.
