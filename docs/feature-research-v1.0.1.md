# FSPEDX / SpeedX — Feature Research Report (v1.0.1)

**Scope:** Research every feature before building. Screens are not a limit — we add as many as needed.
**Target device:** Redmi 14C — Android 14 (HyperOS), MediaTek Helio G81-Ultra, 4GB RAM, 720×1640, 90Hz panel.

---

## 1. Splash Screen Animation

### What the platform offers
- **Android 12+ SplashScreen API** (`androidx.core:core-splashscreen`) — the *official* way. Shows app icon instantly (from the launch theme), then lets you animate it. Requires `Theme.SplashScreen` + `installSplashScreen()`.
- **Legacy approach (works on all versions, incl. our minSdk 26):** a custom Activity with an animated logo — exactly what we have. We animate with `ViewPropertyAnimator` / `ObjectAnimator` / XML `res/anim`.

### Recommended (matches our setup)
- Keep custom `SplashActivity` (Android 14 device, but minSdk 26 = broad support).
- **Logo:** fade-in + scale 0.7→1.0 over 700ms (already added `splash_logo_in.xml`).
- **Tagline/app name:** fade + slide-up 12dp (added `splash_text_in.xml`).
- **Loading bar:** animated indeterminate (already), optionally switch to a determinate sweep (cyan→purple gradient) for a "loading" feel.
- **Transition:** fade out → next screen (already uses `overridePendingTransition`).
- Bonus: subtle logo **pulse** (scale 1.0→1.05→1.0) while the bar loads.

### Buildable? Yes — fully, on all supported versions. Low risk.

---

## 2. Home Dashboard (spread out)

### Research
- Standard Material pattern: a **hero CTA card** + **2×2 stat grid** + **action buttons**.
- Live data already available: ping (`MainViewModel.lastPing`), RAM (`RamCleaner.getRamUsage`), temp/battery (`ThermalUtils`), network (`NetworkUtils.getNetworkType`).
- Redmi 14C is 720px wide → keep tiles large, generous margins, no 3-across cramped text.

### Design
- Top bar: logo (small) + battery% + temp°C.
- Hero card: status pill + ping + RAM progress (already exists — refine spacing).
- 2×2 stat tiles: Ping / RAM / Temp / Battery with icon + value + status dot.
- Bottom: 3 big action buttons — BOOST / RADAR / RECORD.

### Buildable? Yes. Pure layout + wiring to existing data.

---

## 3. Features Hub — Expandable Accordion Cards

### Research
- Material recommends `ExpandableListView` (legacy) or modern **`RecyclerView` with expand/collapse state** — the cleaner, recommended approach.
- Pattern: each card = header (title + "x/y enabled" count + chevron) + collapsible list of toggles inside.
- Toggle count per group can be computed from prefs.

### Grouping (like a phone's settings)
| Group | Features |
|-------|----------|
| Performance | FPS overlay, refresh rate, thermal sentinel, force GPU, disable overlays, turbo/eco profile |
| Network | Wi-Fi lock, smart DNS, dual-channel, VPN tunnel, data starvation, radar |
| Overlays | Floating pill, side panel, crosshair, edge blocking |
| Intelligence | Heatmap, gesture, mis-tap, vibration, crosshair, replay |
| Protection | Whitelist, backup, DND, auto-launch, cache |

### Buildable? Yes. Modify `FeaturesFragment` to use a 2-level model (group → items) with animated expand/collapse (Material `RecyclerView` + `ValueAnimator` on height, or `MaterialCardView` with `LinearLayout` weight).

---

## 4. Game Turbo-style Translucent Mini-Screen (the key feature)

### What Game Turbo actually is
MIUI's Game Turbo shows a **small translucent floating capsule** over the game with: FPS, temp, network ping, and quick toggles (DND, screen record, screenshot, boost). It's a `WindowManager` overlay — exactly the mechanism we already use.

### How to build (verified platform capability)
- **Overlay type:** `TYPE_APPLICATION_OVERLAY` (API 26+) — requires `SYSTEM_ALERT_WINDOW` (already requested in onboarding).
- **Design:** a rounded, semi-transparent card (`#CC0A0A0F` bg, blur if available) ~200×120dp, draggable.
- **Contents:** live FPS + temp + ping row, then 2–3 quick-toggle chips (DND, Record, Boost).
- **Refresh:** every 1s via `Handler` (already have `startUpdatingStats`).
- **Dragging:** update `WindowManager.LayoutParams` (we already fixed this pattern for the pill/crosshair).

### The "mini floating dashboard" = a second, richer overlay alongside the existing pill
- Add a new `GameTurboMiniView` hosted by `OverlayService`.
- Expanded: shows live stats grid + toggles. Collapsed: a small capsule showing just FPS.

### Buildable? **Yes — fully.** We already run overlays; this is a new view + layout reusing existing data pipeline.

---

## 5. Interception Banner (incoming call / notification overlay)

### Research
- This is the riskiest feature on **Android 14**:
  - **Incoming calls:** a third-party app **cannot** render the native call UI or answer/decline calls reliably. `TelecomManager` answer/decline requires being the default dialer or has privileged access. What we CAN do: show our own banner when a call notification arrives (via `NotificationListenerService` + `TelephonyManager` broadcast), but **answering/declining must hand off to the system call UI**.
  - **Notifications:** we already have `NotificationListenerService`; we can read `sbn.notification` and render our own banner with app icon + text.
  - **Bluetooth events:** `BluetoothAdapter.ACTION_STATE_CHANGED` broadcast → banner.

### Realistic scope (be honest)
- ✅ Notification banners (WhatsApp, etc.) — fully buildable.
- ✅ Bluetooth connect/disconnect banner — fully buildable.
- ⚠️ Call banner: can **show** caller info + duration, but **Answer/Decline buttons can only open the system in-call screen** (or require the app to be a default dialer — not acceptable for a booster). We'll route to the system call UI.

### Buildable? Mostly yes, with the call-answer limitation documented. Must keep the phone's call UI as the authority.

---

## 6. Game Lock Guard (edge gesture blocking)

### Research
- A transparent overlay consuming touches near the left/right edges prevents accidental **in-app** back gestures.
- **Important limitation:** Android reserves the gesture **navigation bar** area; an overlay **cannot** block the system Home/Recents gestures (those are handled before the app). What it CAN block: in-app edge-swipe-back in PES and accidental touches at screen edges.
- We already built `EdgeGuardView` — just integrate the toggle (wireframe wants it from the Floating Pill + a Features toggle).

### Buildable? Yes, with the system-gesture limitation documented. On Android 14 gesture-nav, Home/Recents can't be blocked by any non-system app — that's a platform rule, not a code bug.

---

## 7. Floating Pill / Side Panel / Crosshair (enhance translucent)

### Research
- All three already exist and are wired. Enhancements: translucent rounded backgrounds, real data (side panel already pulls real stats), smoother drag.
- Crosshair drag already fixed to use LayoutParams.

### Buildable? Yes — polish only.

---

## 8. Analytics (Heatmap / Mis-taps / Timeline)

### Research
- Heatmap: draw normalized touch dots over `default_controller` image (done). Add empty + loading states.
- Mis-taps: count taps >100px from K-Means centroids (done). Show threshold explanation.
- Timeline: MPAndroidChart line chart (done). Add peak/avg summary + spike coloring + empty state.
- All data from Room (TouchEventDao, LagSpikeDao) — reactive via Flow.

### Buildable? Yes. Polish + states.

---

## 9. Whitelist Dialog (make ADD APP work)

### Research
- Current "Add" just opens general settings — wrong. Correct approach:
  - Launch an **app picker** for the whitelist. Standard options:
    1. `Intent(ACTION_VIEW) → package list` — clunky.
    2. Use `UsageStatsManager.queryUsageStats` to list recently-used apps and let user pick.
    3. **Best:** a custom dialog showing all installed launchable apps (via `PackageManager.getInstalledApplications` + `loadIcon`/`loadLabel`) in a searchable list.
- We'll build option 3 (in-app picker dialog) — no external picker needed.

### Buildable? Yes. New `AppPickerDialog` + wire to `WhitelistAdapter` + persist to `KEY_WHITELIST_APPS`.

---

## 10. Splash / Onboarding / Others — already researched

- Onboarding: auto-advance + per-permission save (fixed). Style polish.
- Backup: works (export/import via ActivityResult). Fine.

---

## Summary Table

| # | Feature | Buildable? | Risk | Notes |
|---|---------|-----------|------|-------|
| 1 | Splash animation | ✅ | Low | ViewPropertyAnimator / XML anims |
| 2 | Home spread-out | ✅ | Low | layout + wiring |
| 3 | Features accordion | ✅ | Low-Med | RecyclerView expand/collapse |
| 4 | Game Turbo mini-screen | ✅ | Low-Med | WindowManager overlay (proven) |
| 5 | Interception banner | ⚠️ partial | Med-High | calls hand off to system UI |
| 6 | Game Lock guard | ✅ (partial) | Low | can't block system gestures |
| 7 | Pill/Side/Crosshair polish | ✅ | Low | reuse existing |
| 8 | Analytics polish | ✅ | Low | data already wired |
| 9 | Whitelist ADD APP | ✅ | Low | custom app picker |
| 10 | Extra screens (any) | ✅ | depends | not a limit |

---

## Research notes / verification limits
- developer.android.com fetches timed out during this session; the above is based on well-established Android platform behavior (WindowManager overlays, SplashScreen API, NotificationListenerService, TelecomManager restrictions, gesture-nav rules) that is stable across Android 8–14.
- The two **hard platform limits** to respect:
  1. **Calls:** answer/decline must go through the system (no third-party can replace the call UI).
  2. **System gestures:** Home/Recents can't be blocked by an app overlay on gesture navigation.
