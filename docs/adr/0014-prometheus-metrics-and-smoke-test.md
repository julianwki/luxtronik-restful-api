# 14. Expose Prometheus metrics and use them as smoke test

- **Status:** Accepted
- **Date:** 2026-10-27

## Context

The service runs unattended. The operator needs to know that it is up. The test suite needs an endpoint that works
without a heat pump.

## Decision

- Install Ktor's `MicrometerMetrics` with a `PrometheusMeterRegistry` and serve it at `GET /metrics`.
- `ApplicationTest` starts the application with the real `application.yaml` and expects `200 OK` from `/metrics`.
- CI (`.github/workflows/build.yml`) runs `./gradlew clean build` on every push with the Java version from
  `gradle.properties`.

## Consequences

- Standard JVM and HTTP metrics without extra code.
- The smoke test checks that configuration, dependency injection and routing work together.
- There are no custom metrics for controller values or adjustment runs yet.
- The smoke test does not cover the controller integration.
