# 12. Use German resource names and simple error responses

- **Status:** Accepted
- **Date:** 2025-05-28 (error handling refined 2026-07-12)

## Context

The controller, its manual and its UI use German terms (Leistungsaufnahme, Arbeitszahl, Warmwasser). The service is
used privately by German-speaking users.

## Decision

- Paths and JSON keys use German domain terms without umlauts: `/leistungsaufnahme`, `/arbeitszahl`,
  `/temperaturen`, `/warmwasserExtra/an|aus|status`, `/offsetHeizen/status|reset`, keys like `heizung`,
  `warmwasser`, `ruecklauf`. Code identifiers outside the domain are English.
- Actions that change state use `POST` (since `03e2040`). Raw register values return `text/plain`.
- Every endpoint catches all exceptions and responds with `500 Internal Server Error` without a body. Details go
  to the log.

## Consequences

- The API matches what users see on the heat pump.
- Non-German speakers have to rely on the endpoint descriptions in `README.md`.
- Consumers cannot tell different errors apart (controller not reachable, label not found, timeout). They have to
  check the logs.
