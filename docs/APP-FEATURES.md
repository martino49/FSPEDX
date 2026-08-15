# FSPEDX — App Features Reference

All features in the current app, what each does, its status, and where it lives.
Status: ✅ working · ⚠️ device/system-dependent · 📅 planned

## Core / Setup

| # | Feature | What it does | Status | Key files |
|---|---------|--------------|--------|-----------|
| 1 | Smart Onboarding Wizard | 4-step permission guide (Overlay, Accessibility, VPN, DND); auto-advances when granted | ✅ | `ui/activities/OnboardingActivity.kt` |

## RAM & Storage

| # | Feature | What it does | Status | Key files |
|---|---------|--------------|--------|-----------|
| 2 | 80% RAM Priority Lock | Force-stops background apps (skips system, Google, whitelist, PES) | ✅ | `utils/performance/RamCleaner.kt` |
| 3 | App Whitelist | Protect apps from the RAM cleaner; add/remove via dialog | ✅ | `WhitelistAdapter.kt`, `FeaturesFragment` |
| 4 | Memory Extension Enabler | Guides to system virtual-RAM settings | ⚠️ | `FeaturesFragment.kt` |
| 5 | Safe Cache Monitor | Clears cache of non-PES apps | ✅ | `FeaturesFragment.kt` |

## Performance

| # | Feature | What it does | Status | Key files |
|---|---------|--------------|--------|-----------|
| 6 | FPS + Temp Overlay | Real FPS via Choreographer + battery temp, top-right | ✅ | `OverlayService.kt`, `view_fps_overlay.xml` |
| 7 | Refresh Rate Switcher | Writes device refresh-rate settings keys; verifies result | ⚠️ device-dependent | `RefreshRateUtils.kt` |
| 8 | Thermal Sentinel | Monitors temp; at critical → forces 60Hz to cut heat | ✅ | `ThermalUtils.kt`, `PerformanceViewModel` |
| 9 | Eco vs Turbo Profiles | Profile selector (Turbo/Eco) persisted | ✅ | `FeaturesFragment` |
| 10 | Auto-Launch Trigger | Detects PES via UsageStatsManager; auto-boost on launch | ✅ | `FSPEDXApplication.kt` |
| 28 | GPU Rendering Tuning | Writes Settings.Global force_gpu_rendering / disable_hw_overlays | ✅ | `PerformanceTuning.kt` |
| — | Keep Screen On | Wake lock while PES foreground (pref-gated) | ✅ | `ScreenKeeper.kt` |

## Network

| # | Feature | What it does | Status | Key files |
|---|---------|--------------|--------|-----------|
| 11 | Wi-Fi Low-Latency Lock | WifiManager WIFI_MODE_FULL_HIGH_PERF lock | ✅ | `NetworkUtils.acquireWifiLock` |
| 12 | Local VPN (UDP priority) | TUN tunnel; only PES allowed; UDP immediate, TCP batched | ✅ | `GameVpnService.kt` |
| 13 | Smart DNS | Cloudflare 1.1.1.1/1.0.0.1 via VPN | ✅ | `GameVpnService.kt` |
| 14 | Dual-Channel Rescue | Opens system network settings | ⚠️ | `FeaturesFragment` |
| 15 | Pre-Match Radar | Ping test (ICMP), colour-coded result | ✅ | `NetworkUtils.ping`, `HomeFragment` |
| 30 | Background Data Starvation | Blocks non-PES sync while PES active (via VPN) | ✅ | `DataGovernor.kt` |

## UI / UX

| # | Feature | What it does | Status | Key files |
|---|---------|--------------|--------|-----------|
| 16 | Smart DND | Silences non-critical notifications while PES active; never cancels calls; restores ringer | ✅ | `NotificationListenerService.kt` |
| 17 | Floating Quick-Pill | Draggable pill: Boost + Record over the game | ✅ | `FloatingPillView.kt`, `OverlayService` |
| 18 | Side Pull-Out Table | Swipe-in dashboard with real live stats + toggles | ✅ | `SidePanelDrawerView.kt` |
| 19 | Local Backup & Restore | Export/Import settings + heatmap + whitelist JSON | ✅ | `BackupManager.kt` |
| 29 | Gesture/Edge Blocking | Transparent edge guard absorbs accidental edge swipes | ✅ | `EdgeGuardView.kt` |
| — | Floating Notifications | Non-critical messages shown as banner over the game (game stays open) | ✅ | `OverlayService.showInterceptionBanner` |

## Intelligence / Analytics

| # | Feature | What it does | Status | Key files |
|---|---------|--------------|--------|-----------|
| 20 | Touch Heatmap Recorder | Accessibility service records taps to Room | ✅ | `TouchAccessibilityService.kt` |
| 21 | Gesture Classification | Tap/swipe/hold classification | ✅ | `TouchAccessibilityService.kt` |
| 22 | Mis-Tap + K-Means | Clusters touch data → suggested button positions + mis-tap count | ✅ | `KMeansCluster.kt`, `AnalyticsViewModel` |
| 23 | Vibration Muscle Training | Persisted toggle for haptic mis-tap feedback | ✅ | `MisTapFragment` |
| 24 | Aiming Crosshair | Draggable neon crosshair overlay | ✅ | `CrosshairOverlayView.kt` |
| 25 | Session Replay | MediaProjection screen recording → MediaStore | ✅ | `SessionRecorderService.kt` |
| 26 | Record Toggle (Pill) | Start/stop recording from pill or Home | ✅ | `FloatingPillView`, `HomeFragment` |
| 27 | Lag Spike Timeline | MPAndroidChart of ping over time | ✅ | `AnalyticsFragment.TimelineFragment` |

## Phase 2

| # | Feature | What it does | Status | Notes |
|---|---------|--------------|--------|-------|
| 31 | Message Floating Window | Quick-reply chat head over game | 📅 | MVP uses MIUI Free Window / nBubble instead |

## Platform limitations (honest)

- **Calls** — answer/decline must go through the system call UI; the app only shows a banner + opens it. (Platform rule, not a bug.)
- **System gestures** — Home/Recents cannot be blocked by an app overlay on gesture navigation. Edge guard only blocks in-app swipes.
- **Refresh rate** — works only if the device exposes the settings keys; the Redmi 14C is a 90Hz panel, so "120→60" is really a 60Hz lock.
- **CPU temp** — some sysfs paths are SELinux-blocked; falls back to battery temp.
