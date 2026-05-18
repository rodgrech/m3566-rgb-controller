$ErrorActionPreference = "Stop"

$apk = Join-Path $PSScriptRoot "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path $apk)) {
    throw "APK not found. Build first with Gradle assembleDebug."
}

& "C:\platform-tools\adb.exe" devices
& "C:\platform-tools\adb.exe" install -r $apk
& "C:\platform-tools\adb.exe" shell monkey -p com.codex.m3566lighttester 1
