# fujitsu-heat-pump-cli

A Kotlin CLI tool to control Fujitsu heat pumps locally via the AirStage WiFi adapter (UTY-TFSXH3), with no cloud dependency.

Communicates directly with the adapter over your local network using its HTTP API.

## Supported Hardware

- **Heat pump:** Fujitsu AGYG14KVCA (other Fujitsu models using the AirStage platform likely work too)
- **WiFi adapter:** UTY-TFSXH3 (USB)

## Features

- Turn the heat pump on/off
- Set target temperature (10.0 - 30.0 C)
- Set operation mode (AUTO, COOL, DRY, FAN, HEAT)
- Set fan speed (AUTO, QUIET, LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH, HIGH)
- Display full device status (power, mode, fan, target/indoor/outdoor temperatures, model)
- Auto-discovery of the adapter on the local network

## Requirements

- JDK 17+
- The WiFi adapter must be connected to your local network

## Quick Start

### Option A: Download the pre-built JAR

```bash
# Download the latest release
curl -L -o fujitsu-heat-pump-cli.jar \
  https://github.com/wizaord/fujitsu-heat-pump-cli/releases/latest/download/fujitsu-heat-pump-cli.jar

# Run it (requires JDK 17+)
java -jar fujitsu-heat-pump-cli.jar status
```

### Option B: Build from source

### 1. Discover the heat pump on your network

```bash
./discover.sh
```

This scans your local subnet, finds the adapter, and saves its IP to `~/.airstage.json`.

### 2. Check status

```bash
./gradlew run --args="status"
```

```
┌─────────────────────────────────────┐
│       État de la PAC AirStage        │
├─────────────────────────────────────┤
│ Alimentation  : OFF                  │
│ Mode          : HEAT                 │
│ Ventilateur   : AUTO                 │
│ Temp. cible   : 22.0°C              │
│ Temp. intér.  : 23.3°C              │
│ Temp. extér.  : 18.0°C              │
│ Modèle        : AGYG14KVCA          │
└─────────────────────────────────────┘
```

### 3. Turn on / off

```bash
./gradlew run --args="on"
./gradlew run --args="off"
```

### 4. Change settings

```bash
# Set temperature
./gradlew run --args="set --temp 23.5"

# Set mode
./gradlew run --args="set --mode COOL"

# Set fan speed
./gradlew run --args="set --fan HIGH"

# Combine multiple settings
./gradlew run --args="set --temp 21.0 --mode HEAT --fan QUIET"
```

## Configuration

The CLI reads `~/.airstage.json`, created automatically by `discover.sh`:

```json
{
  "ip": "192.168.1.15",
  "deviceId": "94XX43E23769",
  "useHttps": false
}
```

- `ip` -- the local IP address of the WiFi adapter
- `deviceId` -- found in the adapter's AP SSID (e.g. `AP-WJ3E-94XX43E23769`)
- `useHttps` -- `true` for HTTPS (self-signed cert), `false` for HTTP (port 80)

## Architecture

The project follows hexagonal architecture (ports & adapters):

```
src/main/kotlin/com/airstage/
|-- domain/
|   |-- model/          # Domain models (PowerState, OperationMode, Temperature, ...)
|   |-- port/
|   |   |-- inbound/    # Use case interface (HeatPumpControlUseCase)
|   |   |-- outbound/   # Gateway & config interfaces
|   |-- service/        # Domain service (HeatPumpService)
|-- adapter/
|   |-- inbound/cli/    # CLI commands (Clikt)
|   |-- outbound/
|       |-- airstage/   # HTTP client for the local AirStage API
|       |-- config/     # JSON file config repository
```

## Testing

Integration tests use a fake AirStage server (Ktor embedded) to test the full CLI end-to-end:

```bash
./gradlew integrationTest
```

## Tech Stack

| Component | Library |
|-----------|---------|
| Language | Kotlin 2.2 |
| Build | Gradle 9.4 (Kotlin DSL) |
| HTTP client | Ktor 3.2 (CIO engine) |
| CLI framework | Clikt 5.0 |
| Serialization | kotlinx.serialization 1.9 |
| Test framework | JUnit 5 + AssertJ |
| Fake server | Ktor Server (CIO, embedded) |

## Local API Notes

Some findings from reverse-engineering the UTY-TFSXH3 local API:

- Endpoints: `POST /GetParam` and `POST /SetParam` (JSON body)
- The adapter returns at most **6 parameters per request** -- requests are automatically batched
- Parameter values:
  - `65535` means "not supported by this model"
  - Target temperature: raw = celsius * 10 (e.g. `220` = 22.0 C)
  - Indoor/outdoor temperature: (raw - 5000) / 100 (e.g. `7050` = 20.5 C)
  - Power: `0` = off, `1` = on
  - Mode: `0` = auto, `1` = cool, `2` = dry, `3` = fan, `4` = heat
- All fields including defaults must be sent in the request body (`device_sub_id`, `set_level`, etc.) -- omitting them causes error `0002`

## License

This project is released into the public domain under [The Unlicense](LICENSE). Do whatever you want with it.
