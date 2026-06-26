<#
.SYNOPSIS
    Builds an installable APK of the myHAB Voice Android client.

.DESCRIPTION
    Wraps the Gradle 'assembleDebug' task and copies the resulting APK to
    client/android/dist/ with a versioned name. The debug APK is signed with the
    local debug keystore (stable per machine), so it installs by sideload and
    updates over previous installs — no Play Store / release signing needed.

.PARAMETER Clean
    Run 'clean' before assembling (slower; use after dependency or NDK changes).

.PARAMETER Install
    After building, install the APK on every connected device via adb.

.EXAMPLE
    .\build-apk.ps1
    .\build-apk.ps1 -Install
    .\build-apk.ps1 -Clean -Install
#>
param(
    [switch]$Clean,
    [switch]$Install
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

# --- version (for a friendly output name) ------------------------------------
$gradleFile = Join-Path $root 'app\build.gradle.kts'
$versionName = '0.0.0'
$m = Select-String -Path $gradleFile -Pattern 'versionName\s*=\s*"([^"]+)"' | Select-Object -First 1
if ($m) { $versionName = $m.Matches[0].Groups[1].Value }

# --- build -------------------------------------------------------------------
$gradlew = Join-Path $root 'gradlew.bat'
$apk = Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'

# Delete the previous APK so packaging rebuilds it from scratch. Incremental
# packaging (zipflinger) appends and leaves the old regions as dead space, which
# can nearly double the file on disk; a fresh package keeps it compact. Compile
# tasks stay incremental, so this is cheap.
if (Test-Path $apk) { Remove-Item $apk -Force }

$tasks = @()
if ($Clean) { $tasks += 'clean' }
$tasks += ':app:assembleDebug'

Write-Host "Building myHAB Voice $versionName (debug)..." -ForegroundColor Cyan
Push-Location $root
try {
    & $gradlew @tasks
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed (exit $LASTEXITCODE)" }
} finally {
    Pop-Location
}

# --- collect the APK ---------------------------------------------------------
if (-not (Test-Path $apk)) { throw "APK not found at $apk" }

$dist = Join-Path $root 'dist'
New-Item -ItemType Directory -Force -Path $dist | Out-Null
$outName = "myhab-voice-$versionName-debug.apk"
$out = Join-Path $dist $outName
Copy-Item -Path $apk -Destination $out -Force

$sizeMb = [math]::Round((Get-Item $out).Length / 1MB, 1)
Write-Host "APK ready: $out ($sizeMb MB)" -ForegroundColor Green

# --- optional install --------------------------------------------------------
if ($Install) {
    $adb = 'adb'
    if ($env:ANDROID_HOME) {
        $candidate = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
        if (Test-Path $candidate) { $adb = $candidate }
    }

    $devices = & $adb devices | Select-Object -Skip 1 |
        Where-Object { $_ -match '\tdevice$' } |
        ForEach-Object { ($_ -split '\t')[0] }

    if (-not $devices) {
        Write-Warning "No connected devices (check USB debugging). Skipping install."
    } else {
        foreach ($d in $devices) {
            Write-Host "Installing on $d..." -ForegroundColor Cyan
            & $adb -s $d install -r $out
        }
    }
}
