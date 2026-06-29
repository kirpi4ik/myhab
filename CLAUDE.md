# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

myHAB (My Home Automation Backend) — a full-stack home automation platform. Grails 6.1/Groovy backend + Vue 3/Quasar PWA frontend + a native Android voice-assistant client. Manages IoT devices (ESP32, MEGAD), solar inverters (Huawei), heat pumps (NIBE), weather data, and automation scenarios via MQTT, GraphQL, and WebSocket.

## Build & Run Commands

### Backend (Grails/Gradle)
```bash
./gradlew bootRun                        # Run Grails backend (port 8181)
./gradlew test                           # Run all server tests (Spock)
./gradlew server-core:test               # Run server-core tests only
./gradlew server-core:test --tests "org.myhab.services.StatisticsServiceSpec"  # Single test class
./gradlew assembleServerAndClient        # Build combined JAR (server + client)
./gradlew buildImage                     # Build Docker image
./gradlew clean copyClientResources assembleServerAndClient createDockerfile  # Full CI build
```

### Frontend (Vue 3 + Quasar)
```bash
cd client/web-vue3
yarn install                             # Install dependencies
yarn serve                               # Dev server on port 10002
yarn pwa:dev                             # PWA dev mode on port 10002
yarn build                               # Production SPA build
yarn pwa:build                           # Production PWA build
yarn lint                                # ESLint
yarn format                              # Prettier
```

### Gradle shortcuts (from root)
```bash
./gradlew serve                          # Run SPA dev server
./gradlew servePWA                       # Run PWA dev server
```

### Android voice client (standalone — NOT part of the root build)
```bash
cd client/android
./gradlew :app:assembleDebug             # build debug APK (arm64-only)
./build-apk.ps1                          # build + copy versioned APK to dist/ (Windows/PowerShell)
./build-apk.ps1 -Install                 # also adb-install on connected devices
./build-apk.ps1 -Clean                   # clean rebuild (after NDK/dependency changes)
```
`client/android/` has its own Gradle wrapper (Gradle 8.9 / AGP 8.5) because the Android Gradle Plugin is incompatible with the root Grails multi-module build (Gradle 7.x). **Open only `client/android` in Android Studio**, not the repo root. First native build is slow + needs internet (CMake `FetchContent` pulls tflite-micro et al.).

## Architecture

### Multi-module Gradle project (root build)
- `:server-core` — Main Grails 6.1 application (controllers, domain, services, jobs)
- `:server-config` — Configuration key constants and provider interface
- `:server-rules` — Business rules engine (heating, lighting facts)
- `:web-vue3` — Vue 3 + Quasar frontend

> The native Android client (`client/android/`) is a **separate** Gradle build, intentionally excluded from the root `settings.gradle` (see "Native Android voice client" below).

### Backend structure (`server/server-core/grails-app/`)
- **controllers/** — REST endpoints + `UrlMappings.groovy` for routing. GraphQL is the primary API.
- **domain/** — GORM domain classes (~80). Key hierarchies: `device/` (Device, DevicePort, DevicePeripheral, Cable), `infra/` (Zone, Layer), `job/` (Job, Scenario, EventDefinition, CronTrigger), `auth/` (User, Role, AccessToken), `events/` (MQTTTopic)
- **services/** — Business logic. Key subsystems: `graphql/` (schema factory, fetchers for Query/Mutation), `async/mqtt/` (MQTT integration via Spring Integration), `async/socket/` (WebSocket), `dsl/` (automation DSL), `telegram/` (bot handler)
- **jobs/** — Quartz scheduled jobs (13 jobs). Intervals configured in `application.yml` under `myhab.job.*`
- **conf/application.yml** — Server port (8181), Quartz job intervals, CORS origins, database config

### Frontend structure (`client/web-vue3/src/`)
- **boot/** — App initialization (axios, Apollo GraphQL client, i18n, PWA, error handler)
- **pages/** — Route-level components. `public/` contains unauthenticated pages
- **components/** — Reusable Vue components
- **graphql/** — GraphQL query/mutation definitions
- **composables/** — Vue 3 composables
- **_services/** and **_helpers/** — Service layer and utilities

### Native Android voice client (`client/android/`)
A standalone, hands-free Android companion (Kotlin + Jetpack Compose, minSdk 30 / Android 11) — a thin client over the **existing** server contract, no backend changes. Flow: wake word → on-device STT → `voiceCommand` GraphQL mutation → play the server's neural-TTS MP3 reply (falls back to Android `TextToSpeech`).
- **Modules:** `:app` (`org.myhab.voice`) and `:microwakeword` — the wake-word NDK engine vendored from Home Assistant (Apache-2.0): real TF `audio_microfrontend` + TFLite-Micro via JNI, built for `arm64-v8a` only (real devices; no emulator).
- **Auth:** native username/password → `POST /api/login` → JWT stored in Keystore-backed `EncryptedSharedPreferences`; re-login on `401`.
- **Networking:** OkHttp + kotlinx.serialization (no Apollo). Default backend `https://madhouse.app`, overridable in Settings (use `http://10.0.2.2:8181` for the emulator).
- **Wake word:** microWakeWord model + config in `app/src/main/assets/` (`wake_word.json` + `<model>.tflite`, git-ignored); runs in a foreground microphone service.
- **Audio routing gotcha:** replies play via the legacy `STREAM_MUSIC` path (not `AudioAttributes`), which mis-routes to the earpiece on some One UI builds — see `voice/ReplyPlayer.kt`.
- Full details + setup in `client/android/README.md`.

### Key communication patterns
1. **Device ↔ Server**: MQTT via Spring Integration (`MqttTopicService` routes messages, `MqttPublishGateway` publishes)
2. **Client ↔ Server**: GraphQL (Apollo Client) for queries/mutations; WebSocket (STOMP) for real-time updates
3. **Scheduled sync**: Quartz jobs poll external APIs (Huawei solar, NIBE heat pump, Open-Meteo weather) and publish state via MQTT

### GraphQL schema
Defined in `server/server-core/src/main/resources/schema.graphqls`. Custom fetchers live in `services/graphql/fetchers/` (Query.groovy, Mutation.groovy). GORM domain classes are auto-registered via `GORMSchemaRegistry`.

### Event system
Events flow through named topics (e.g., `EVT_LIGHT`, `EVT_GATE`) with parameters p0–p6. `EventDefinition` and `EventSubscription` domain classes define event routing. Scenarios can trigger on event patterns.

## Tech Stack & Requirements

- **Java 17** (required — Temurin recommended)
- **Node.js 18+** with Yarn (frontend)
- **Grails 6.1.0** / Groovy 3.0.25 / GORM 7.3.4
- **PostgreSQL** database
- **Vue 3** / Quasar 2.18 / Apollo Client 3.14
- **Spring Security Core 6.0** with OAuth 2.0 token auth
- **Hazelcast 5.3** for caching
- **Android client:** Kotlin 1.9 / Jetpack Compose / minSdk 30 (Android 11) — standalone Gradle 8.9 + AGP 8.5, NDK + CMake for the native wake-word module

## Testing

Backend tests use **Spock** framework:
- Unit tests: `server/server-core/src/test/groovy/`
- Integration tests: `server/server-core/src/integration-test/groovy/`
- Code style: CodeNarc rules in `server/server-core/config/codenarc/rules.groovy`

Frontend linting: ESLint (`.eslintrc.js`) + Prettier (`.prettierrc.js`)

## CI/CD

GitHub Actions (`.github/workflows/gradle.yml`): triggers on push/PR to master or beta. Builds combined JAR, creates Docker image (`eclipse-temurin:17-jre` base), pushes to DockerHub as `kirpi4ik/myhab:{version}-{sha}`.

## Configuration

- Backend config: `server/server-core/grails-app/conf/application.yml` (job intervals, CORS, DB)
- Runtime config: `server/server-core/grails-app/conf/runtime.groovy`
- Spring beans: `server/server-core/grails-app/conf/spring/resources.groovy`
- Frontend config: `client/web-vue3/quasar.config.js` (build modes, plugins, dev proxy)
- Frontend env: `client/web-vue3/src/.env`
- Android client: `client/android/app/build.gradle.kts` (SDK/version), `gradle/libs.versions.toml` (version catalog); runtime base URL/language/wake/speaker are app settings persisted in `EncryptedSharedPreferences` (`data/Session.kt`)
