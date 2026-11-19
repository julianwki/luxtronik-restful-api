# 6. Add random noise to the current power consumption

- **Status:** Accepted
- **Date:** 2025-07-03

## Context

The controller reports the current power consumption in kW with limited precision. The API returns it in watts.

## Decision

When the current power consumption is greater than 0, it is converted to watts and a random value between −5 and
+4 W is added (`asFuzzyWatt()` in `PowerConsumptionService`). A value of 0 stays 0. Only the current value of
`/leistungsaufnahme` is affected, not the totals and not the COP calculation.

The reason is not recorded in the code or commit messages (`95a904d`: "bring some fuzzyness into current
consumption"). A likely reason is that some consumers only record or forward a value when it changes, so a constant
value would look like missing data. **This should be confirmed by the owner.**

## Consequences

- Consumers always see small changes while the heat pump runs.
- The current value is not exactly what the controller reports. The error is at most 5 W.
- Two calls to the same uncached value can return different numbers.
