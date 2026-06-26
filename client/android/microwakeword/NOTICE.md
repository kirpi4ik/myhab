# microwakeword module — attribution

This module is vendored from the **Home Assistant Companion for Android**
project and is licensed under the **Apache License, Version 2.0**.

- Source: https://github.com/home-assistant/android/tree/main/microwakeword
- License: https://github.com/home-assistant/android/blob/main/LICENSE.md (Apache-2.0)

It provides on-device wake-word detection: TensorFlow Lite's `audio_microfrontend`
feature extractor plus the TFLite-Micro interpreter, compiled from source via the
NDK (CMake `FetchContent` pulls `tflite-micro` and `kissfft` at build time) and
exposed to Kotlin through a small JNI layer.

## Local modifications

- `src/main/cpp/CMakeLists.txt`: disabled the optional debug-only HWAddressSanitizer
  block (it requires extra APK packaging / a HWASan-capable setup not used here).
- `build.gradle.kts`: rewritten as a standalone Android library (the upstream uses
  Home Assistant's internal convention plugins) — `compileSdk 34`, `minSdk 30`,
  ABI filters `arm64-v8a` + `x86_64`, CMake 3.22.1, Timber dependency.

The C/C++ sources and `MicroWakeWord.kt` (package
`io.homeassistant.companion.android.microwakeword`) are unchanged — the package is
preserved because the JNI layer binds methods to that exact class name.
