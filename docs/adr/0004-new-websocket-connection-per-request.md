# 4. Open a new WebSocket connection per request

- **Status:** Accepted
- **Date:** 2025-07-03 (refined 2025-07-08, 2026-02-09)

## Context

Keeping a long-lived WebSocket session to the controller requires reconnect handling, keeping the login alive and
dealing with a controller that may close idle connections. Early versions reused a client, which led to problems
(see commits `95a904d`, `e0f7a18`, `5cc6c5b`).

## Decision

For every data request, `WebsocketService.retrieveData` creates a new `HttpClient`, opens a WebSocket, logs in,
runs the caller's block, and closes session and client. The whole exchange runs in `withTimeout(2.seconds)`.

## Consequences

- Simple and robust: no shared state, no reconnect logic, a broken session only affects one request.
- Every request pays for a TCP connect, the login and the navigation tree. The cache from
  [ADR-0005](0005-cache-responses-for-10-seconds.md) keeps this cost bounded.
- A slow controller makes requests fail after 2 seconds instead of hanging.
