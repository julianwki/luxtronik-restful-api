# 3. Read data through the WebSocket interface by menu names

- **Status:** Accepted
- **Date:** 2025-06-30

## Context

The Luxtronik controller has a WebSocket interface (subprotocol `Lux_WS`) used by its web UI. After `LOGIN;<pin>`
it sends a navigation tree. `GET;<id>` returns the content of one menu page as name/value pairs, with units as text
(e.g. `1.2 kW`). The IDs of the menu entries are not stable between sessions or firmware versions.

## Decision

- Read all display values (power consumption, heat quantity, temperatures, operating state) through the WebSocket.
- Find pages by their **German menu path** (e.g. `Informationen › Energiemonitor › Leistungsaufnahme`) using
  `Navigation.readNavigationId`, and values by their **label** using `Content.readValue`/`readDouble`.
- Trim whitespace from names and strip the unit suffix before parsing numbers.

## Consequences

- No hard-coded IDs. Changing IDs do not break the service.
- The service depends on the German UI language and on exact labels. A renamed label or a new value format causes
  an exception and a `500` response (this happened with firmware v3.92.1, see commit `a6819c8`).
- The WebSocket gives access to computed values (e.g. energy monitor) that are not available as Modbus registers.
