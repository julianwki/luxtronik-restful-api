# 8. Use Modbus TCP to read and write settings

- **Status:** Accepted
- **Date:** 2026-07-05

## Context

Writing settings (hot water boost, heating mode, heating offset) through the WebSocket UI protocol is undocumented
and fragile. The controller also offers Modbus TCP with documented registers.

## Decision

- Use **j2mod** to access Modbus TCP, unit ID `1`.
- `SingleRegisterModbusService` reads and writes one register per call. It connects, runs the transaction and
  closes the connection every time (3 s timeout).
- All methods are `@Synchronized`, so only one Modbus transaction runs at a time.
- Registers are constants in the code: `10071` (hot water boost), `10000` (heating mode), `10002` (heating offset,
  × 10), input register `10108` (outside temperature, × 10).

## Consequences

- Writes use a documented, stable interface.
- Two protocols to the same controller, each with its own port ([ADR-0013](0013-configure-through-environment-variables.md)).
- Serialized access avoids conflicts but blocks a thread while waiting.
- Register numbers depend on the controller's Modbus map and cannot be changed without a new build.
