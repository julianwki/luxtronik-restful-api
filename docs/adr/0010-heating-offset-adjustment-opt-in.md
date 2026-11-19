# 10. Make writing the heating offset opt-in

- **Status:** Accepted
- **Date:** 2026-10-28

## Context

The heating offset adjustment ([ADR-0009](0009-forecast-based-heating-offset-adjustment.md)) changes heating
settings of a real heating system without user interaction. A wrong value can make the house too cold or waste
energy.

## Decision

The property `service.heatingOffsetAdjustment.active` (`HEATING_OFFSET_ADJUSTMENT`) defaults to `false`. When it is
`false`, the job still fetches the forecast, reads the sensor and logs the calculated offset, but returns before any
write. `POST /offsetHeizen/reset` also does not write.

## Consequences

- A new installation is safe by default.
- The operator can watch the calculated offsets in the logs (dry run) before turning the feature on.
- When disabled, the reset endpoint returns `200 OK` without changing anything, which can be surprising.
