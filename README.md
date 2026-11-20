# luxtronik-restful-api

A small [Ktor](https://ktor.io) server that exposes data from a heat pump with a **Luxtronik 2.x** controller as a
RESTful JSON API. It also runs a background job that adjusts the heating curve offset based on a weather forecast.

The controller is accessed via two interfaces:

- **WebSocket** (`Lux_WS` protocol, default port `8214`) – reading status values (power consumption, coefficient of
  performance, temperatures)
- **Modbus TCP** (default port `502`) – reading and writing single registers (hot water boost, heating mode and offset)

## Endpoints

All data endpoints are cached for 10 seconds. On any error the server responds with `500 Internal Server Error`.

| Method | Path                      | Response     | Description                                                                 |
|--------|---------------------------|--------------|-----------------------------------------------------------------------------|
| `GET`  | `/leistungsaufnahme`      | JSON         | Power consumption: current (W) and total (kWh) for heating, hot water, sum  |
| `GET`  | `/arbeitszahl`            | JSON         | Coefficient of performance: current and total for heating, hot water, sum   |
| `GET`  | `/temperaturen`           | JSON         | Flow, return, hot gas, outside, hot water, solar temperatures (°C)          |
| `GET`  | `/warmwasserExtra/status` | `text/plain` | Status of the hot water boost (Modbus register `10071`, `1` = on, `0` = off) |
| `POST` | `/warmwasserExtra/an`     | –            | Activate the hot water boost                                                |
| `POST` | `/warmwasserExtra/aus`    | –            | Deactivate the hot water boost                                              |
| `GET`  | `/offsetHeizen/status`    | `text/plain` | Current heating offset in Kelvin (Modbus register `10002`)                  |
| `POST` | `/offsetHeizen/reset`     | –            | Set heating mode to off and the heating offset to `0` K                     |
| `GET`  | `/metrics`                | `text/plain` | Prometheus metrics                                                          |

Example requests can be found in [`requests.http`](requests.http) (IntelliJ HTTP client). Copy
`http-client.env.json.example` to `http-client.env.json` and adjust the hosts.

### Example responses

`GET /leistungsaufnahme`

```json
{
  "aktuell": { "heizung": 1203.0, "warmwasser": 0.0, "gesamt": 1203.0 },
  "gesamt": { "heizung": 4321.5, "warmwasser": 1234.5, "gesamt": 5556.0 }
}
```

`GET /arbeitszahl`

```json
{
  "aktuell": { "heizung": 4.12, "warmwasser": 0.0, "gesamt": 4.12 },
  "gesamt": { "heizung": 4.3, "warmwasser": 3.1, "gesamt": 4.0 }
}
```

`GET /temperaturen`

```json
{
  "vorlauf": 32.1,
  "ruecklauf": 28.4,
  "ruecklaufSoll": 29.0,
  "heissgas": 61.2,
  "aussentemperatur": 4.3,
  "mitteltemperatur": 5.1,
  "warmwasserIst": 47.8,
  "warmwasserSoll": 48.0,
  "solarkollektor": 5.0,
  "solarspeicher": 150.0
}
```

The current values are split between heating and hot water depending on the operating state (`Betriebszustand`):
in state `HEIZEN` everything is attributed to heating, in `WW` to hot water, otherwise it is split evenly.

## Heating offset adjustment

`HeatingOffsetAdjustmentService` runs one minute after startup and then every 15 minutes (at :00, :15, :30, :45):

1. Fetch the current temperature for the configured location from [Open-Meteo](https://open-meteo.com).
2. Read the outside temperature measured by the heat pump (Modbus input register `10108`).
3. Calculate the offset as `local − forecast`, truncated toward zero to 0.5 K steps (e.g. `1.3` → `1.0`,
   `−1.3` → `−1.0`).
4. If the offset is `0`, reset heating mode and offset. Otherwise, set heating mode to *offset* (register `10000` =
   `2`) and write the offset to register `10002` if it changed.

Which forecast temperature is used is configured with `HEATING_OFFSET_TEMPERATURE`:

| Value                  | Description                                            |
|------------------------|--------------------------------------------------------|
| `all` (default)        | Average of `temperature_2m` and `apparent_temperature` |
| `temperature_2m`       | Air temperature at 2 m                                 |
| `apparent_temperature` | Apparent ("feels like") temperature                    |

The adjustment is **disabled by default** (`HEATING_OFFSET_ADJUSTMENT=false`). In that case the offset is still
calculated and logged, but nothing is written to the heat pump – this also applies to `POST /offsetHeizen/reset`.

## Configuration

Configuration lives in [`application.yaml`](src/main/resources/application.yaml) and can be overridden with environment
variables:

| Variable                     | Default         | Description                                                |
|------------------------------|-----------------|------------------------------------------------------------|
| `LUXTRONIK_HOST`             | `127.0.0.1`     | Host of the Luxtronik controller (WebSocket and Modbus)    |
| `LUXTRONIK_WS_PORT`          | `8214`          | WebSocket port of the controller                           |
| `LUXTRONIK_MODBUS_PORT`      | `502`           | Modbus TCP port of the controller                          |
| `LUXTRONIK_PIN`              | `999999`        | PIN for the WebSocket login                                |
| `LOCATION_LAT`               | `53.2525`       | Latitude used for the weather forecast                     |
| `LOCATION_LON`               | `10.4144`       | Longitude used for the weather forecast                    |
| `TZ`                         | `Europe/Berlin` | Timezone used for the weather forecast                     |
| `HEATING_OFFSET_ADJUSTMENT`  | `false`         | Enable writing the heating offset to the heat pump         |
| `HEATING_OFFSET_TEMPERATURE` | `all`           | Forecast temperature to use (see above)                    |

The server listens on port `8080`.

## Building & Running

Requires Java 21.

| Task                                    | Description                                                          |
|-----------------------------------------|----------------------------------------------------------------------|
| `./gradlew test`                        | Run the tests                                                        |
| `./gradlew build`                       | Build everything                                                     |
| `./gradlew run`                         | Run the server                                                       |
| `./gradlew buildFatJar`                 | Build an executable JAR of the server with all dependencies included |
| `./gradlew buildImage`                  | Build the docker image to use with the fat JAR                       |
| `./gradlew publishImageToLocalRegistry` | Publish the docker image (`luxtronik-restful-api`) locally           |
| `./gradlew runDocker`                   | Run using the local docker image                                     |

Example with Docker:

```shell
./gradlew publishImageToLocalRegistry
docker run -d -p 8080:8080 \
  -e LUXTRONIK_HOST=192.168.1.10 \
  -e HEATING_OFFSET_ADJUSTMENT=true \
  luxtronik-restful-api:<version>
```

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```

## Documentation

- [Product Requirements Document](docs/PRD.md) – goals, requirements and known limitations
- [Architecture Decision Records](docs/adr/README.md) – why the service is built the way it is

## License

This project is licensed under the [PolyForm Noncommercial License 1.0.0](LICENSE.md). You may use, change and
share it for any noncommercial purpose, such as running it for your own heat pump, hobby projects or use by
non-profit organizations. Commercial use is not permitted. For a commercial license, please contact the author.

## Funding

If this project is useful to you, you can support its development on Ko-fi:

[![Support me on Ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/julianwki)

## Tech stack

- [Ktor](https://ktor.io) (Netty server, CIO client, WebSockets, kotlinx.serialization)
- [Koin](https://insert-koin.io) with KSP annotations for dependency injection
- [j2mod](https://github.com/steveohara/j2mod) for Modbus TCP
- [ktor-simple-cache](https://github.com/ucasoft/ktor-simple-cache) for response caching
- [Micrometer](https://micrometer.io) / Prometheus for metrics
- [Kotest](https://kotest.io) for testing
