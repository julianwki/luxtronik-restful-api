# 7. Split current values by operating state

- **Status:** Accepted
- **Date:** 2025-07-02 (fallback changed 2025-12-03, enum since 2026-01-30)

## Context

The controller reports one current power consumption and one current heat output. It does not split them into
heating and hot water. It does report the operating state (`Betriebszustand`), e.g. `Heizen`, `WW`, `Abt`.

## Decision

The enum `DataService.Betriebszustand` decides how current values are assigned:

| State | Heating | Hot water |
|---|---|---|
| `HEIZEN` | 100 % | 0 |
| `WW` | 0 | 100 % |
| `ABT` (defrost) and unknown | 50 % | 50 % |

The same enum calculates the COP per mode. The COP for the mode that is not active is `0.0`. For the totals,
`UNDEFINED` is used, so heating, hot water and overall COP are all calculated from the controller's totals.

## Consequences

- Consumers get separate current values for heating and hot water without extra logic.
- The 50/50 split for defrost and unknown states is an approximation, not a measurement.
- New operating states fall back to `UNDEFINED` without errors.
