# FSPEDX Documentation Index

Everything you need to understand, build, test, and ship FSPEDX / SpeedX.

## Core docs

| Doc | Purpose |
|-----|---------|
| [Architecture](ARCHITECTURE.md) | Code structure, packages, data flow, services |
| [App Features](APP-FEATURES.md) | All 30+ features, what each does, status, files |
| [App Screens](APP-SCREENS.md) | Every screen/overlay, its purpose, current state |
| [Development](DEVELOPMENT.md) | Build, install to device, run, test |
| [Feature Research (v1.0.1)](feature-research-v1.0.1.md) | Research + feasibility of each planned feature |
| [UI Improvement Plan (v1.0.1)](v1.0.1-UI-Improvement-Plan.md) | Planned UI/UX upgrades |
| [Wireframe](wireframe.md) | Raw wireframe capture (18 screens) |
| [Product Acceptance Questions](product-acceptance-questions.md) | "Would I keep using this?" checklist |

## Per-feature docs

- [`features/`](features/README.md) — one markdown file per feature (1–31) with
  category, description, implementation file, performance confidence.

## Quick answers

- **What is this?** A PES game booster for budget Android phones (Redmi 14C).
- **Current version focus:** PES / eFootball only (v1.0.1).
- **Does it build?** Yes — `gradle assembleDebug` (JDK 17, Gradle 8.5, SDK 34).
- **Does it run on device?** Yes — installed on Redmi 14C via adb, verified no-crash on launch and Home.
- **Is there fake data?** No — all UI data is real; placeholders show "--" until real values arrive.
