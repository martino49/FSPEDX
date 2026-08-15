# FSPEDX — Development Guide

How to build, install, and test FSPEDX.

## Prerequisites

- **JDK 17** — e.g. `C:\Users\Derivo\DevTools\jdk-17.0.20+8`
- **Gradle 8.5** — e.g. `C:\Users\Derivo\DevTools\gradle-8.5` (AGP 8.3.2 requires ≥8.5)
- **Android SDK Platform 34** — `C:\Users\Derivo\AppData\Local\Android\Sdk`
- **adb** — `C:\Users\Derivo\AppData\Local\Android\Sdk\platform-tools\adb.exe`
- Target device: Redmi 14C (`2409BRN2CA`), Android 14

`local.properties` already points at the SDK; `gradle/wrapper/gradle-wrapper.properties`
points at Gradle 8.5.

## Build

```powershell
$env:JAVA_HOME = "C:\Users\Derivo\DevTools\jdk-17.0.20+8"
$env:ANDROID_HOME = "C:\Users\Derivo\AppData\Local\Android\Sdk"
Set-Location C:\Users\Derivo\Documents\FSPEDX
& "C:\Users\Derivo\DevTools\gradle-8.5\bin\gradle.bat" assembleDebug
```

Output: `app\build\outputs\apk\debug\app-debug.apk`

### Common build errors & fixes

| Symptom | Cause / fix |
|---------|-------------|
| `Cannot read field "elmName" because "root" is null` | A UTF-8 **BOM** in an XML file. Strip it (must start with `<?xml`). |
| `Minimum supported Gradle version is 8.6` | AGP is too new for the installed Gradle. (We use AGP 8.3.2 + Gradle 8.5.) |
| `Unresolved reference: R` | Missing `import com.fspedx.booster.R` in the Kotlin file. |
| `app:icon` not found | The layout is missing `xmlns:app="http://schemas.android.com/apk/res-auto"`. |

## Install to device (adb)

```powershell
$adb = "C:\Users\Derivo\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb devices -l                       # confirm 2409BRN2CA is listed
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
```

- MIUI may show an install confirmation — if you get
  `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`, simply retry.
- A fresh install starts at Onboarding; complete the 4 permissions once.

### Grant permissions for testing (via adb)

```powershell
$pkg = "com.fspedx.booster"
$adb = "C:\Users\Derivo\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb shell "appops set $pkg SYSTEM_ALERT_WINDOW allow"
& $adb shell "appops set $pkg GET_USAGE_STATS allow"
& $adb shell "appops set $pkg WRITE_SETTINGS allow"
& $adb shell "settings put secure enabled_notification_listeners $pkg/com.fspedx.booster.services.NotificationListenerService"
& $adb shell "settings put secure enabled_accessibility_services $pkg/com.fspedx.booster.services.TouchAccessibilityService"
& $adb shell "settings put secure accessibility_enabled 1"
```

## Launch & verify

```powershell
$adb = "C:\Users\Derivo\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb shell "am start -n com.fspedx.booster/.ui.activities.SplashActivity"
& $adb shell "dumpsys window | grep mCurrentFocus"   # expect MainActivity (Home)
& $adb shell pidof com.fspedx.booster                # expect a PID (process alive)
```

## Testing checklist (quick)

1. Fresh install → onboarding completes → Home loads, no crash.
2. Numbers on Home update live (battery, temp, RAM, ping, network).
3. Each tab (Features / Analytics / Gallery) opens without crash.
4. Enabling overlays (pill, FPS, edge guard) shows them; disabling removes them.
5. BOOST reports MB freed; RADAR reports a ping; RECORD requests permission and records.
6. Keep-screen-on holds while PES is foreground; releases after.
7. A notification while PES is foreground → banner over the game, game stays open.
8. Verify no crash in `adb logcat -s AndroidRuntime:E`.

## Verification of fake-data-free UI

Every visible value must be real. Placeholders use `--` (not fake numbers).
Search for regressions:

```powershell
# Should return nothing for emoji
Select-String -Path app\src\main\java\**\*.kt -Pattern "[\u2600-\u27BF]"
```

See `docs/product-acceptance-questions.md` for the full acceptance checklist.
