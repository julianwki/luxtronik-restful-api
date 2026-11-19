# Architecture Decision Records

This directory records the architecture decisions of luxtronik-restful-api. See
[ADR-0001](0001-record-architecture-decisions.md) for the format. ADRs 0002–0015 were written afterwards from the code
and git history.

| ADR | Title | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-use-ktor-with-koin.md) | Use Ktor with Koin for the HTTP server | Accepted |
| [0003](0003-read-data-via-websocket-menu-navigation.md) | Read data through the WebSocket interface by menu names | Accepted |
| [0004](0004-new-websocket-connection-per-request.md) | Open a new WebSocket connection per request | Accepted |
| [0005](0005-cache-responses-for-10-seconds.md) | Cache data responses in memory for 10 seconds | Accepted |
| [0006](0006-add-noise-to-current-power-consumption.md) | Add random noise to the current power consumption | Accepted |
| [0007](0007-split-current-values-by-operating-state.md) | Split current values by operating state | Accepted |
| [0008](0008-use-modbus-tcp-for-writes.md) | Use Modbus TCP to read and write settings | Accepted |
| [0009](0009-forecast-based-heating-offset-adjustment.md) | Adjust the heating offset based on a weather forecast | Accepted |
| [0010](0010-heating-offset-adjustment-opt-in.md) | Make writing the heating offset opt-in | Accepted |
| [0011](0011-average-forecast-temperature-by-default.md) | Use the average of air and apparent temperature by default | Accepted |
| [0012](0012-german-api-names-and-simple-error-handling.md) | Use German resource names and simple error responses | Accepted |
| [0013](0013-configure-through-environment-variables.md) | Configure through environment variables with defaults | Accepted |
| [0014](0014-prometheus-metrics-and-smoke-test.md) | Expose Prometheus metrics and use them as smoke test | Accepted |
| [0015](0015-container-image-with-ktor-gradle-plugin.md) | Build the container image with the Ktor Gradle plugin | Accepted |
