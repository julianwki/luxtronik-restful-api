# Product Requirements Document: luxtronik-restful-api

| | |
|---|---|
| **Status** | Reverse-engineered from code at `3a4f19d` (version `0.8.1-beta`) |
| **Owner** | Julian Wiersbitzki |
| **Last updated** | 2026-11-18 |

## 1. Summary

luxtronik-restful-api is a small, self-hosted HTTP service that sits next to a heat pump with a **Luxtronik 2.x**
controller. It turns the controller's proprietary interfaces (WebSocket and Modbus TCP) into a simple JSON/REST API
for home automation, dashboards and monitoring. It also lowers the heating demand when the heat pump's own outside
sensor reads colder than the actual weather by adjusting the heating curve offset based on a weather forecast.

## 2. Problem

- The Luxtronik controller has no documented REST API. Its values are available through a WebSocket protocol
  (`Lux_WS`) that mirrors the controller's German menu tree, and through Modbus TCP registers.
- Home automation systems and dashboards need simple, stable endpoints that return numbers, not menu trees.
- Derived values that the controller does not show directly (coefficient of performance per operating mode, power
  consumption split into heating and hot water) have to be calculated.
- The outside temperature sensor of the heat pump can differ from the real outside temperature (e.g. because of
  where it is mounted). The heating curve then heats more or less than needed.

## 3. Goals

1. Provide read-only JSON endpoints for power consumption, coefficient of performance (COP) and temperatures.
2. Allow switching the hot water boost ("Warmwasser Extra") on and off.
3. Automatically correct the heating curve offset using a weather forecast, with a safe opt-in.
4. Run unattended in a container on the home network, with metrics for monitoring.

## 4. Non-goals

- A user interface. Consumers are other systems (home automation, dashboards, Prometheus).
- Authentication, authorization or exposure to the internet. The service is meant for a trusted home network.
- Support for several heat pumps per instance, or for controllers other than Luxtronik 2.x.
- Storing history. Long-term data is the job of the consumer (e.g. Prometheus or the home automation system).
- A full mapping of all controller values or Modbus registers.

## 5. Users

| User | Need |
|---|---|
| Home owner / operator | Sees consumption, efficiency and temperatures of the heat pump; triggers hot water boost |
| Home automation system | Polls JSON endpoints, triggers actions via `POST` |
| Monitoring (Prometheus) | Scrapes `/metrics` for service health |

## 6. Functional requirements

### 6.1 Reading data (WebSocket)

| ID | Requirement |
|---|---|
| FR-1 | `GET /leistungsaufnahme` returns the current power consumption in watts (`aktuell`) and the total energy consumption in kWh (`gesamt`), each split into `heizung`, `warmwasser` and `gesamt`. |
| FR-2 | The current power consumption is assigned according to the operating state: `HEIZEN` → all to heating, `WW` → all to hot water, any other state (e.g. `ABT` defrost) → split 50/50. |
| FR-3 | `GET /arbeitszahl` returns the current COP (heat output ÷ power consumption) and the total COP (heat quantity ÷ energy consumption) for `heizung`, `warmwasser` and `gesamt`, rounded to 2 decimals. Division by zero returns `0.0`. |
| FR-4 | `GET /temperaturen` returns flow, return, return target, hot gas, outside, mean, hot water actual/target, solar collector and solar storage temperatures in °C. |
| FR-5 | Values are found by their German menu names in the controller's navigation tree (e.g. `Informationen › Energiemonitor › Leistungsaufnahme`), so that changing menu IDs between firmware versions do not break the service. |
| FR-6 | Responses of FR-1, FR-3 and FR-4 are cached for 10 seconds. |

### 6.2 Hot water boost (Modbus)

| ID | Requirement |
|---|---|
| FR-7 | `GET /warmwasserExtra/status` returns the raw value of holding register `10071` as plain text (`1` = on, `0` = off). |
| FR-8 | `POST /warmwasserExtra/an` writes `1`, `POST /warmwasserExtra/aus` writes `0` to register `10071`. |

### 6.3 Heating offset adjustment

| ID | Requirement |
|---|---|
| FR-9 | A background job runs 1 minute after start, then at every quarter hour (:00, :15, :30, :45) in local time. |
| FR-10 | The job fetches the current temperature for the configured location and timezone from Open-Meteo. |
| FR-11 | The forecast temperature is configurable: `temperature_2m`, `apparent_temperature` or `all` (average of both, default). It is rounded to 1 decimal. |
| FR-12 | The job reads the heat pump's outside temperature from input register `10108` (value ÷ 10). |
| FR-13 | Target offset = local − forecast, truncated toward zero to steps of 0.5 K. |
| FR-14 | If the target offset is `0`, heating mode (register `10000`) and offset (register `10002`) are reset to `0`. Otherwise, heating mode is set to `2` (offset) if needed, and the offset (× 10) is written only if it differs from the current value. |
| FR-15 | Writing is disabled unless `HEATING_OFFSET_ADJUSTMENT=true`. When disabled, the job still calculates and logs the offset (dry run). |
| FR-16 | `GET /offsetHeizen/status` returns the current offset in Kelvin as plain text. `POST /offsetHeizen/reset` resets mode and offset (only when writing is enabled). |
| FR-17 | A failing run is logged and does not stop the schedule. |

### 6.4 Operations

| ID | Requirement |
|---|---|
| FR-18 | `GET /metrics` exposes Prometheus metrics (JVM and HTTP server metrics via Micrometer). |
| FR-19 | Any error while handling a request returns `500 Internal Server Error` without a body. Details are logged. |
| FR-20 | Requests are logged when they do not return `200 OK`. In development mode, all requests are logged. |

## 7. Non-functional requirements

| ID | Requirement |
|---|---|
| NFR-1 | **Timeouts:** a WebSocket session must finish within 2 seconds, a Modbus connection times out after 3 seconds. |
| NFR-2 | **Controller load:** the service opens a connection per request and closes it afterwards. The 10-second cache limits the load created by polling consumers. Modbus access is serialized. |
| NFR-3 | **Safety:** automatic writes to heating settings are opt-in (FR-15). |
| NFR-4 | **Deployment:** single container (Java 21), listens on port `8080`, configured only through environment variables. |
| NFR-5 | **Quality:** CI builds and tests on every push. A smoke test checks that the application starts and serves `/metrics`. |

## 8. Configuration

| Variable | Default | Purpose |
|---|---|---|
| `LUXTRONIK_HOST` | `127.0.0.1` | Controller host |
| `LUXTRONIK_WS_PORT` | `8214` | WebSocket port |
| `LUXTRONIK_MODBUS_PORT` | `502` | Modbus TCP port |
| `LUXTRONIK_PIN` | `999999` | WebSocket login PIN |
| `LOCATION_LAT` / `LOCATION_LON` | `53.2525` / `10.4144` | Location for the forecast |
| `TZ` | `Europe/Berlin` | Timezone for the forecast |
| `HEATING_OFFSET_ADJUSTMENT` | `false` | Enable writing the heating offset |
| `HEATING_OFFSET_TEMPERATURE` | `all` | Forecast temperature to use |
| `LOG_LEVEL` | `info` | Root log level |

## 9. External dependencies and assumptions

- **Luxtronik controller** with WebSocket (`Lux_WS`) and Modbus TCP enabled, Modbus unit ID `1`. The WebSocket
  model was last adapted to firmware **v3.92.1**.
- The controller's menu uses German names (`Informationen`, `Anlagenstatus`, `Betriebszustand`, …) and values carry
  unit suffixes (`kW`, `kWh`, `°C`).
- **Open-Meteo** (`api.open-meteo.com/v1/forecast`) is reachable without an API key.
- The service runs in a trusted network. Anyone who can reach it can switch the hot water boost and reset the offset.

## 10. Known limitations and open questions

1. **No authentication.** Acceptable for the home network, but it must never be exposed publicly.
2. **Firmware coupling.** A renamed menu entry or a changed value format breaks the related endpoint with `500`.
3. **Fixed registers.** Modbus registers (`10000`, `10002`, `10071`, `10108`) are hard-coded and not configurable.
4. **No offset limits.** The calculated offset is not clamped to a range. Should there be a maximum (e.g. ±5 K)?
5. **Random noise on current power.** The current power consumption has a random ±5 W added
   (see [ADR-0006](adr/0006-add-noise-to-current-power-consumption.md)). The reason is not documented in the code.
6. **No retry** for failed forecast or Modbus calls. The next scheduled run is the retry.
7. **Test coverage** covers the menu parser and a startup smoke test only. Calculation logic (COP, offset
   rounding, operating-state split) has no tests.

## 11. Architecture decisions

See [`docs/adr`](adr/README.md).
