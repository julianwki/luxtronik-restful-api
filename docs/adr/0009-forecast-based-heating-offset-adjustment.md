# 9. Adjust the heating offset based on a weather forecast

- **Status:** Accepted
- **Date:** 2026-10-20

## Context

The heating curve uses the outside temperature measured by the heat pump's own sensor. If that sensor reads too
cold or too warm (position, sun, wind), the heat pump heats too much or too little.

## Decision

`HeatingOffsetAdjustmentService` corrects the heating curve with the controller's offset function:

- **Source:** current temperature from Open-Meteo for the configured location (free, no API key).
- **Formula:** offset = local sensor − forecast, truncated toward zero to 0.5 K steps. A colder sensor gives a
  negative offset, so the heat pump heats less.
- **Writes:** if the offset is 0, heating mode and offset are reset. Otherwise heating mode is set to "offset" (`2`)
  and the offset is written only when it changed.
- **Schedule:** a coroutine started in `configureScheduling()` runs 1 minute after start, then at every quarter
  hour. Errors are logged and the loop continues.

## Consequences

- The heating follows the real weather more closely.
- Depends on an external service. If Open-Meteo is not reachable, the run fails and the last offset stays in place
  until the next successful run.
- Small differences (< 0.5 K) do not cause writes, which reduces writes to the controller.
- The offset has no upper or lower limit.
- The scheduler is in-process and has no persistence. Missed runs (e.g. during a restart) are not made up.
