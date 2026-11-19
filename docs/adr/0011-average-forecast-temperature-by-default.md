# 11. Use the average of air and apparent temperature by default

- **Status:** Accepted
- **Date:** 2026-10-29 (supersedes the default `apparent_temperature` from 2026-10-28)

## Context

Open-Meteo provides `temperature_2m` (air temperature) and `apparent_temperature` (includes wind and humidity).
Air temperature ignores wind chill, which increases a building's heat loss. Apparent temperature can overreact,
for example to strong wind.

## Decision

The forecast temperature is configurable through `HEATING_OFFSET_TEMPERATURE` (enum `Temperature`):
`temperature_2m`, `apparent_temperature` or `all`. The default is `all`, the average of both values, rounded to
1 decimal.

## Consequences

- The default is a compromise between both values.
- Operators can choose a pure value without a new build.
- An unknown value in the configuration makes the service fail at startup.
