<div align="center">

![FSPEDX / SpeedX](fspedx-logo-transparent.png)

# FSPEDX • SpeedX

**Full-Speed Extreme** — Game performance booster for PES / eFootball on the Redmi 14C.

</div>

FSPEDX (SpeedX) is an Android game booster built for budget phones like the Redmi 14C
(4 GB RAM, MediaTek Helio G81-Ultra). It combines networking, performance, touch
analytics and in-game overlays to make the most of limited hardware.

## Features (30 locked + 1 later)

| # | Category | Feature | Confidence |
|---|----------|---------|------------|
| 1 | Setup | Smart Onboarding Wizard | – |
| 2 | RAM | 80% Priority Lock + Force-Stop Background Apps | Medium |
| 3 | RAM | App Whitelist (Protect WhatsApp/Music from being killed) | Medium |
| 4 | RAM | Memory Extension Enabler (Virtual RAM guide) | Medium |
| 5 | RAM | Safe Cache Monitor (Junk cleaner excluding PES) | Medium |
| 6 | Perf | FPS + Temperature Overlay | High |
| 7 | Perf | Refresh Rate Smart Switcher (120Hz → 60Hz) | High |
| 8 | Perf | Thermal Sentinel (Auto-throttle protection) | High |
| 9 | Perf | Eco vs Turbo Profiles | – |
| 10 | Perf | Auto-Launch Trigger (Detects PES start) | High |
| 11 | Network | Wi-Fi Low-Latency Lock | High |
| 12 | Network | Local VPN (UDP Priority + Bandwidth Hijack) | High |
| 13 | Network | Smart DNS (Cloudflare/Google) | Medium |
| 14 | Network | Dual-Channel Rescue (WiFi + Mobile Data) | Medium |
| 15 | Network | Pre-Match Network Radar (Ping test) | High |
| 16 | UI/UX | Smart DND (Mute calls/notifications) | Medium |
| 17 | UI/UX | Floating Quick-Pill (In-game controls) | High |
| 18 | UI/UX | Side Pull-Out Table (Live Stats Dashboard) | High |
| 19 | UI/UX | Local Backup & Restore | High |
| 20 | Intel | Touch Heatmap Recorder | High |
| 21 | Intel | Gesture Classification (Tap/Swipe/Hold) | High |
| 22 | Intel | Mis-Tap Detection + K-Means Clustering | Medium |
| 23 | Intel | Vibration Muscle Training | Medium |
| 24 | Intel | Aiming Crosshair Overlay | Low |
| 25 | Intel | Session Replay (Screen recording + overlay) | High |
| 26 | Intel | Record Toggle on Floating Pill | High |
| 27 | Analytics | Lag Spike Timeline (Post-match report) | High |
| 28 | Perf | GPU Rendering Tuning (Force GPU + Disable HW Overlays) | High |
| 29 | UX | Gesture / Edge Blocking (Sticky Immersive Mode) | High |
| 30 | Perf | Background Data Starvation (Restrict sync) | High |
| 31 | UX | Message Floating Window (Quick Reply) — Phase 2 | – |

Confidence = how likely the feature improves real gameplay performance on the
Redmi 14C. High = direct hardware/network manipulation, Medium = temporary or
conditional, Low = skill / UX aid.

## How It Helps

- **Ping:** 10–30 ms lower and more stable (Wi-Fi lock + VPN UDP priority + DNS).
- **FPS stability:** fewer drops (30 → 15 fps moments) via thermal management and
  60 Hz refresh switching.
- **Heat:** reduced throttling means longer consistent sessions.

It will **not** turn a Helio G81 into a flagship — no software can add GPU power.

## Project Layout

```
app/src/main/java/com/fspedx/booster/
├── FSPEDXApplication.kt        App class + Auto-Launch trigger
├── ui/                         Activities, fragments, adapters, custom views
├── services/                   VPN, Overlay, Accessibility, DND, Recorder
├── viewmodels/                 MVVM state holders
├── data/                       Room database, models, repository
├── utils/                      Permissions, network, performance, ML, backup
└── di/                         Manual DI (AppContainer)

docs/features/                  One markdown page per feature (1–31)
```

## Tech Stack

Kotlin • AndroidX • Material Components • Room • Coroutines • MPAndroidChart •
OkHttp • PermissionX • Gson • ViewBinding

## Building

1. Open the project in Android Studio (uses the bundled JDK).
2. Let it sync Gradle (8.7) — Android Studio will download the distribution
   the first time and regenerate the wrapper.
3. SDK Platform 34 is already installed on this machine.
4. Build → Run on your Redmi 14C.

## Notes

- Package: `com.fspedx.booster`
- minSdk 26 (Android 8.0), targetSdk 34
- Logo: `fspedx-logo-transparent.png` (background removed from the source PNG)