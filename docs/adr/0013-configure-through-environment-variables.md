# 13. Configure through environment variables with defaults

- **Status:** Accepted
- **Date:** 2025-07-13 (separate ports since 2026-11-18)

## Context

The service runs as a container. Host, PIN and location differ per installation and should not be part of the
image.

## Decision

- All settings are in `src/main/resources/application.yaml` using Ktor's `"$ENV_VAR:default"` syntax.
- Each service has a `Config` data class built from `ApplicationConfig`, registered in Koin.
- WebSocket and Modbus ports have separate variables (`LUXTRONIK_WS_PORT`, `LUXTRONIK_MODBUS_PORT`). The earlier
  shared `LUXTRONIK_PORT` set both ports, which could not work because they differ (commit `a28d014`).
- The log level is set with `LOG_LEVEL` in `logback.xml`.

## Consequences

- One image works for every installation.
- Defaults allow local start without any variable (e.g. the smoke test).
- Invalid values (e.g. a non-number port) fail at startup, not later.
- Renaming a variable is a breaking change for existing deployments.
