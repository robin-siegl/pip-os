# Run with USB debugging enabled and the app installed.
$ErrorActionPreference = 'Stop'
$packageName = 'dev.pipos.launcher'
$devices = & adb devices
if ($LASTEXITCODE -ne 0 -or -not ($devices -match '\tdevice')) {
    throw 'No authorized Android device found. Check USB debugging and adb devices.'
}
Write-Host '=== PIP-OS activity startup ==='
& adb shell am force-stop $packageName
& adb shell am start -W -n "$packageName/.MainActivity"
Write-Host '=== PIP-OS process memory ==='
& adb shell dumpsys meminfo $packageName
Write-Host '=== PIP-OS frame diagnostics ==='
& adb shell dumpsys gfxinfo $packageName framestats
Write-Host '=== End. Review startup/memory/frame data and perform the manual checklist from README. ==='
