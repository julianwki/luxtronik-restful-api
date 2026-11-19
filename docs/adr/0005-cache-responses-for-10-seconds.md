# 5. Cache data responses in memory for 10 seconds

- **Status:** Accepted
- **Date:** 2025-07-05 (10 s for all endpoints since 2026-01-30)

## Context

Consumers such as home automation systems poll the endpoints often. Each request opens a WebSocket session to the
controller ([ADR-0004](0004-new-websocket-connection-per-request.md)), which is an embedded device with limited
resources. The values change slowly.

## Decision

Use `ktor-simple-cache` with an in-memory cache. `/leistungsaufnahme`, `/arbeitszahl` and `/temperaturen` are
cached for 10 seconds (`cacheOutput(10.seconds)`). Modbus endpoints and `/metrics` are not cached.

## Consequences

- At most one controller session per data endpoint every 10 seconds, no matter how many consumers poll.
- Data can be up to 10 seconds old.
- The cache is lost on restart and not shared between instances, which is fine for a single instance.
