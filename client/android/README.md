# myHAB Voice — native Android client

A minimal, hands-free Android companion for the myHAB voice assistant. It is
woken by a wake word ("Hey myHAB"), captures one spoken command, runs it through
the **existing** `voiceCommand` GraphQL mutation, and speaks the reply. All
intent parsing, device execution and neural TTS happen server-side — this app is
a thin front-end over that contract (no backend changes).

## What it does

```
(wake word | tap-to-talk) → on-device speech-to-text → voiceCommand mutation
   → play server MP3 reply (fallback: Android TextToSpeech)
   → if the assistant asked something, auto-listen again (same conversation)
```

- **Auth:** native username/password → `POST /api/login` → JWT stored in
  Keystore-backed `EncryptedSharedPreferences`. Re-prompts login on `401`
  (the backend issues ~10h JWTs and uses no refresh token).
- **Wake word:** [microWakeWord](https://github.com/kahrendt/microWakeWord) running
  in the native `:microwakeword` module (vendored from Home Assistant, Apache-2.0:
  the real TF `audio_microfrontend` + TFLite-Micro compiled via the NDK and exposed
  through JNI). Runs in a foreground microphone service (survives screen-off), fully
  offline, **no account or access key**. Paused during a turn so the mic is never
  contended, then resumed.
- **STT:** Android `SpeechRecognizer` (on-device, language from Settings —
  `en-US` / `ro-RO`). The server API takes text, not audio, exactly like the web
  client.
- **TTS:** plays the server's base64 MP3 (`audioContent`); falls back to Android
  `TextToSpeech` when absent.
- **UI:** Home (status, conversation log, tap-to-talk, wake toggle) and Settings
  (server URL, language, wake toggle, speaker output, logout).

## Requirements

- Android Studio (Koala/Ladybug or newer) — provisions the Android SDK.
- minSdk 30 (Android 11), compileSdk/targetSdk 34. Java 17 toolchain.
- **NDK + CMake** (for the `:microwakeword` native module) — Android Studio prompts
  to install them on first sync. The first native build is slow and needs internet:
  CMake `FetchContent` downloads tflite-micro, kissfft, flatbuffers, gemmlowp, ruy.
- A reachable myHAB server with `feature.voice.enabled=true` and an LLM key set.

## Build & run

```bash
cd client/android
./gradlew :app:assembleDebug          # build the debug APK
./gradlew :app:installDebug           # install on a connected device/emulator
```

### Sideload APK (no Play Store)

For installing on your own devices, `build-apk.ps1` (Windows PowerShell) wraps the
debug build and drops a versioned APK in `dist/`:

```powershell
.\build-apk.ps1                       # → dist/myhab-voice-<version>-debug.apk
.\build-apk.ps1 -Install              # also adb-installs on connected devices
.\build-apk.ps1 -Clean -Install       # clean build (after NDK/dependency changes)
```

The debug APK is signed with the local debug keystore, so updates install over
previous installs. No release signing is needed unless you publish to a store.

The Gradle wrapper points at Gradle 8.9 (downloaded on first run). Android Studio
will create `local.properties` with your SDK path automatically; from the CLI,
set `ANDROID_HOME`/`sdk.dir` first.

## One-time setup

1. **Server URL** — on the login screen (and Settings). Defaults to
   `http://10.0.2.2:8181` (emulator → host loopback). On a real device, use the
   server's LAN address, e.g. `http://192.168.1.50:8181`. Cleartext HTTP is
   allowed for LAN dev (`usesCleartextTraffic="true"`); use HTTPS in production.
2. **Wake word (optional)** — bundle a microWakeWord model (no account/key):
   - Grab a ready-made model + config from
     [esphome/micro-wake-word-models](https://github.com/esphome/micro-wake-word-models)
     (e.g. `okay_nabu`, `hey_jarvis`) — or train a custom "Hey myHAB" with the
     [microWakeWord trainer](https://github.com/kahrendt/microWakeWord).
   - Put `wake_word.json` + its `.tflite` in `app/src/main/assets/`
     (see `assets/README.txt` for the exact JSON shape and filenames).
   - The `.tflite` is git-ignored (large/swappable). Without it, tap-to-talk still
     works; only the wake word is disabled.

   > **Why not Picovoice/Porcupine?** Picovoice discontinues its free tier on
   > 2026-06-30 (enterprise-only after; the SDK refuses to run without a valid
   > key even offline), so this app uses the open-source microWakeWord instead.

## Server contract (reference)

- `POST {baseUrl}/api/login` → `{ access_token, ... }`
- `POST {baseUrl}/graphql`, header `Authorization: Bearer <jwt>`
- `voiceCommand(transcript, locale, sessionId)` → `VoiceCommandResult`
  (`spokenResponse`, `audioContent`/`audioMime`, `awaitingReply`, `sessionId`,
  `actions`). See `server/server-core/src/main/resources/schema.graphqls` and the
  web reference `client/web-vue3/src/pages/VoiceControl.vue`.

## Project map

| Area | Files |
|------|-------|
| Secure storage / state | `data/Prefs.kt`, `data/Session.kt` |
| Networking | `net/AuthApi.kt`, `net/VoiceApi.kt`, `net/Dto.kt`, `net/Http.kt` |
| Voice pipeline | `voice/WakeWordService.kt`, `voice/WakeWordEngine.kt`, `voice/SpeechRecognizerController.kt`, `voice/ReplyPlayer.kt`, `voice/VoiceController.kt` |
| Wake-word native engine | `:microwakeword` module (vendored, Apache-2.0 — see `microwakeword/NOTICE.md`) |
| UI | `MainActivity.kt`, `ui/LoginScreen.kt`, `ui/HomeScreen.kt`, `ui/SettingsScreen.kt`, `ui/theme/Theme.kt` |
