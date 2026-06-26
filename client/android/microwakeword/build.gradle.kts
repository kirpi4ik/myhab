// Vendored from the Home Assistant Android companion app (Apache-2.0):
// https://github.com/home-assistant/android/tree/main/microwakeword
// On-device wake-word detection: TF microfrontend + TFLite-Micro compiled via the
// NDK (sources fetched by CMake FetchContent), exposed to Kotlin through JNI.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.homeassistant.companion.android.microwakeword"
    compileSdk = 34

    defaultConfig {
        minSdk = 30

        // App ships only to real (arm64) phones, never an emulator — so build the
        // (heavy) native tflite-micro for arm64 only. Add "x86_64" here if you ever
        // need to run on an x86_64 emulator.
        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                // Android 15+ 16 KB page-size compatibility.
                arguments += "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.timber)
}
