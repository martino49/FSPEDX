# FSPEDX — Architecture

## Overview

FSPEDX is a **PES/eFootball performance booster** for budget Android phones.
It runs persistent system **overlays** over the game, a **local VPN** for network
priority, an **accessibility service** for touch capture, and a **notification
listener** for DND/interception — all managed from a standard MVVM UI.

- Package: `com.fspedx.booster`
- minSdk 26 · targetSdk 34 · compileSdk 34
- Kotlin · AndroidX · Material Components · Room · Coroutines · MPAndroidChart

## Layers

```
┌──────────────────────────────────────────────────────────────┐
│  UI  (activities / fragments / adapters / custom views)      │
│       Splash · Onboarding · Main (4 tabs) · dialogs          │
├──────────────────────────────────────────────────────────────┤
│  ViewModels  (Main / Network / Performance / Analytics)      │
│       StateFlow + LiveData to UI                             │
├──────────────────────────────────────────────────────────────┤
│  Services (long-running)                                     │
│   GameVpnService · OverlayService · SessionRecorderService   │
│   TouchAccessibilityService · NotificationListenerService    │
├──────────────────────────────────────────────────────────────┤
│  Repository  (StatsRepository) → Room (AppDatabase)          │
├──────────────────────────────────────────────────────────────┤
│  Utils (objects)                                             │
│   network · performance · backup · permissions · ml · screen │
├──────────────────────────────────────────────────────────────┤
│  DI  (AppContainer — manual singletons)                      │
└──────────────────────────────────────────────────────────────┘
```

## Package breakdown

| Package | Contents |
|---------|----------|
| `ui.activities` | `SplashActivity`, `OnboardingActivity`, `MainActivity` |
| `ui.fragments` | `HomeFragment`, `FeaturesFragment`, `AnalyticsFragment` (+3 inner tabs), `GalleryFragment` |
| `ui.adapters` | `OnboardingPagerAdapter`, `WhitelistAdapter`, `ReplayGridAdapter` |
| `ui.customviews` | `FloatingPillView`, `CrosshairOverlayView`, `SidePanelDrawerView`, `EdgeGuardView` |
| `services` | `GameVpnService`, `OverlayService`, `TouchAccessibilityService`, `NotificationListenerService`, `SessionRecorderService` |
| `viewmodels` | `MainViewModel`, `NetworkViewModel`, `PerformanceViewModel`, `AnalyticsViewModel` |
| `data.database` | `AppDatabase`, `TouchEventDao`, `LagSpikeDao`, `Converters` |
| `data.models` | `TouchEvent`, `LagSpike`, `NetworkStats`, `PerformanceStats` |
| `data.repository` | `StatsRepository` |
| `utils.constants` | `Constants` (package names, prefs keys, intents) |
| `utils.network` | `NetworkUtils`, `TrafficInterceptor`, `DataGovernor` |
| `utils.performance` | `RamCleaner`, `ThermalUtils`, `RefreshRateUtils`, `ScreenKeeper`, `PerformanceTuning` |
| `utils.backup` | `BackupManager` |
| `utils.permissions` | `PermissionHelper` |
| `utils.ml` | `KMeansCluster` |
| `di` | `AppContainer` (manual DI) |

## Data flow

### Live stats (Home dashboard)
```
ThermalUtils / RamCleaner / NetworkUtils / OverlayService (FPS)
        └──────────────► HomeFragment.updateStats()  (every 1s)
```
- Battery level, temperature, RAM used/total, network type, FPS all read live.
- Ping comes from `MainViewModel.lastPing` (updated by `runPingTest()`).

### Touch heatmap pipeline
```
TouchAccessibilityService.onAccessibilityEvent()
   ├─ TYPE_VIEW_CLICKED → save TouchEvent(x,y,type) to Room
   └─ TYPE_TOUCH_INTERACTION_START/END → classify tap/swipe/hold
Room (touch_events)
   └─ AnalyticsViewModel.loadTouchEvents() → HeatmapFragment draws dots
        └─ KMeansCluster.findCentroids() → "Suggested positions"
             └─ detectMisTaps() → mis-tap count + locations
```

### Network
```
GameVpnService (VpnService)
   ├─ Builder.addAllowedApplication(PES variants) + setBlocking(true)
   ├─ addDnsServer(1.1.1.1)  [Smart DNS]
   ├─ Wi-Fi low-latency WifiLock
   └─ Packet loop: UDP forwarded immediately, TCP batched (priority)
DataGovernor  (Background Data Starvation)
   └─ When PES foreground + pref on → ensure VPN running
```

### Overlays (OverlayService)
Hosts all persistent overlays as `TYPE_APPLICATION_OVERLAY` windows:
- FPS/Temp counter (top-right) — updated by a real `Choreographer` frame callback
- Floating pill (draggable; Boost + Record)
- Side panel (swipe-in dashboard)
- Crosshair (draggable)
- Edge guard (transparent, absorbs edge touches)
- Interception banner (floating notifications/calls over the game)

Dragging an overlay updates its `WindowManager.LayoutParams` (required for overlays).

### DND / Interception (NotificationListenerService)
- While PES is foreground:
  - Calls are NEVER cancelled (route to system call UI).
  - Non-critical notifications → shown as a **floating banner** over the game
    (game stays open), or muted if the banner is disabled.
- Ringer is only touched while PES is active and always restored.

### Screen keeping
`ScreenKeeper` (PowerManager) holds a wake lock while PES is foreground **and**
`KEY_KEEP_SCREEN_ON` is enabled. Released on exit.

### Auto-launch
`FSPEDXApplication.startForegroundMonitoring()` polls `UsageStatsManager` every 2s;
when PES enters the foreground it triggers auto-boost + data governor + screen keeper.

## Services & lifecycle

| Service | Type | Foreground | Start |
|---------|------|-----------|-------|
| GameVpnService | VpnService | yes | startForegroundService |
| OverlayService | Service | yes (specialUse) | startForegroundService |
| SessionRecorderService | Service | yes (mediaProjection\|microphone) | startForegroundService |
| TouchAccessibilityService | AccessibilityService | n/a | system |
| NotificationListenerService | NotificationListener | n/a | system |

All started via `startForegroundService` (Android 8+ safe) and call
`startForeground()` promptly (within 5s).

## Configuration

- `build.gradle.kts` — AGP 8.3.2, Kotlin 1.9.24, compileSdk 34, Java 17, Room via kapt
- `AndroidManifest.xml` — permissions incl. WRITE_SETTINGS, WAKE_LOCK, FOREGROUND_SERVICE_*
- `Constants` — PES package names (incl. `jp.konami.pesam`), prefs keys, intent actions
