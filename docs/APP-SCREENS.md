# FSPEDX — App Screens Reference

Complete inventory of screens and persistent overlays. Based on the 18-screen
wireframe (`docs/wireframe.md`) and the current build.

## Activities

| Screen | File | Purpose | Status |
|--------|------|---------|--------|
| Splash | `activity_splash.xml` / `SplashActivity.kt` | Logo + tagline + loading bar; routes to Onboarding or Main | ✅ (animation resources ready, wiring pending) |
| Onboarding | `activity_onboarding.xml` / `OnboardingActivity.kt` | 4-step permission wizard; auto-advances | ✅ |
| Main Hub | `activity_main.xml` / `MainActivity.kt` | Bottom-nav host for 4 fragments | ✅ |

## Main fragments (bottom nav)

| # | Screen | File | Purpose | Status |
|---|--------|------|---------|--------|
| 4 | Home Dashboard | `fragment_home.xml` / `HomeFragment.kt` | Top bar (logo/battery/temp), hero status card, 2×2 stat tiles, BOOST/RADAR/RECORD | ✅ redesigned |
| 5 | Features Hub | `fragment_features.xml` / `FeaturesFragment.kt` | All feature toggles grouped by section | ⚠️ flat list — accordion planned |
| 6 | Analytics Studio | `fragment_analytics.xml` / `AnalyticsFragment.kt` | 3-tab host (Heatmap/Mis-taps/Timeline) | ✅ |
| 7 | Replay Gallery | `fragment_gallery.xml` / `GalleryFragment.kt` | Grid of recorded sessions | ✅ |

## Analytics sub-tabs

| # | Screen | File | Purpose | Status |
|---|--------|------|---------|--------|
| 8 | Heatmap | `fragment_heatmap.xml` | Touch density over controller image + K-Means suggestions | ✅ |
| 9 | Mis-taps | `fragment_mistaps.xml` | Mis-tap count + vibration training | ✅ |
| 10 | Timeline | `fragment_timeline.xml` | Ping vs time chart (MPAndroidChart) | ✅ |

## Dialogs

| # | Screen | File | Purpose | Status |
|---|--------|------|---------|--------|
| 11 | Whitelist | `dialog_whitelist.xml` | Protected apps list + remove | ✅ (ADD APP picker to be improved) |
| 12 | Backup | `FeaturesFragment.showBackupDialog` | Export/Import JSON | ✅ |

## Persistent overlays (over any app while enabled)

| # | Screen | File | Position | Status |
|---|--------|------|----------|--------|
| 13 | FPS/Temp Counter | `view_fps_overlay.xml` | Top-right | ✅ real FPS |
| 14 | Floating Quick-Pill | `view_floating_pill.xml` / `FloatingPillView.kt` | Right edge, draggable | ✅ |
| 15 | Side Pull-Out Dashboard | `view_side_panel.xml` / `SidePanelDrawerView.kt` | Slides from left | ✅ real data |
| 16 | Aiming Crosshair | `CrosshairOverlayView.kt` | Center, draggable | ✅ |
| 17 | Interception Banner | `view_interception_banner.xml` | Top, slides down | ✅ |
| 18 | Game Lock Guard | `EdgeGuardView.kt` | Left/right edges (transparent) | ✅ |

## Planned improvements (v1.0.1)

- **Home**: already redesigned (top bar, hero card, 2×2 tiles, icon buttons).
- **Features**: convert flat list → expandable accordion cards with counts.
- **Splash**: wire the fade/scale animations into `SplashActivity`.
- **Analytics/Gallery**: empty states, chart polish.
- **Game Turbo-style mini dashboard**: small translucent draggable window with
  FPS/temp/network + quick toggles (planned, all three variants requested).
