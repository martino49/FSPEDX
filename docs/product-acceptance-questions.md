# FSPEDX — Product & Strategy Questions (v1.0.1 · PES-only focus)

**Scope locked:** This release is PES / eFootball ONLY.
(We keep the architecture clean so a future game profile can slot in, but we do NOT build generic-game features now.)

Every question each stakeholder asks before "this is good, I'll keep using it" — for the PES-focused v1.0.1.

---

## A. END USER (the PES player)

### First launch
- Does the app open fast and look finished (not a dev build)?
- Do I understand what it does in 5 seconds — "makes PES smoother on my phone"?
- Is onboarding short and does it explain WHY each permission is needed?
- Can I skip onboarding and still use the app?
- If I deny a permission, does it tell me instead of failing silently?

### The launch moment (make-or-break)
- When I open PES, does FSPEDX visibly DO something? (overlay appears? "FSPEDX active" notice? auto-boost?)
- When I close PES, do things cleanly turn off (no stuck overlay, no battery drain)?
- Does it EVER close or crash my game? (Hard no.)
- Does it drain battery when idle?

### Home / dashboard
- Do ping, RAM, temp, FPS update live and make sense?
- Can I tell at a glance if I'm "ready to play"?
- Do buttons respond and report results (BOOST shows MB freed)?
- Is any number fake or stuck (user WILL notice)?

### Features (the 30)
- Do the PES-relevant features actually work? (Wi-Fi lock, VPN, RAM, thermal, keep-screen-on, DND, overlays)
- Are any toggles dead / do nothing?
- Is it clear which need which permission?

### In-game (PES)
- Does the floating pill stay reachable but not block PES controls?
- Does the mini-dashboard show real FPS/temp while playing?
- Notification/call arrives → I see it over the game without leaving PES?
- Can I record a session and replay it with the heatmap?
- Does the edge guard stop accidental back-swipes but not break PES?

### Trust
- Are permissions justified? Is there a privacy note?
- Can I back up / wipe data?
- Survives a full 30-min match without dying?

### "Keep using" signal
- After ONE match, did PES feel smoother? (That's the only reason to keep it.)
- No crash mid-match, no battery drain, no lies in the UI.

---

## B. PRODUCT TEAM

- One-sentence pitch: "Makes PES run smoother on budget phones"?
- Which 5 PES features deliver 90% of value? (Focus there.)
- What is the hero moment the player tells friends about?
- What brings them back: session gallery, heatmap evolution, ping history?
- Free vs paid for this PES-focused version? (What's the paid tier value?)
- Does the name/icon/UI feel trustworthy enough to pay for?

---

## C. TESTING / QA (PES, Android 14, Redmi 14C)

### Install & env
- Fresh install → onboarding completes?
- Install over existing → data preserved?
- PES not installed → no crash, clear message?
- Low storage / low battery / weak Wi-Fi → no crash?

### Permissions matrix
- All granted → everything works?
- Each denied individually → graceful, explained, retryable?
- Revoked mid-session → no crash?

### Lifecycle
- App killed / backgrounded → services clean up?
- Screen off during PES → clean behavior?
- Call comes in during PES → game NOT closed, banner shows, system handles call?

### Data correctness
- Ping matches a real ICMP ping?
- RAM matches system?
- FPS matches device reality (not always 60)?
- Thermal sentinel actually does something at 45°C?
- Heatmap/mis-taps reflect REAL taps (not zeros/random)?
- Timeline shows real ping samples with correct times?

### Overlays
- Appear over PES only, not other apps?
- Removed cleanly on stop (no ghosts)?
- Edge guard blocks in-app swipes, not system nav?
- 30+ min PES session stable?

### Recording
- MediaProjection flow works end-to-end?
- File saved to MediaStore, shows in Gallery?
- No crash on long recording?

---

## D. DEVELOPERS

- Clean architecture; no dead toggles / fake data / placeholders?
- All data real (DB/system), strings externalized?
- Consistent design system or ad-hoc?
- Suspend functions in coroutines (no main-thread blocking)?
- Services proper foreground (startForeground ≤5s)?
- Android 14 manifest/permissions correct?
- Overlays type-correct with fallbacks?
- VPN never breaks PES connectivity?
- Idle CPU/RAM/battery minimal; polling stops when it should?
- No secrets committed; reproducible build; can ship a release APK?

---

## E. THE SHIP GATE (PES v1.0.1)

1. Fresh install → onboarding → Home, no crash, no confusion.
2. Launching PES triggers a VISIBLE, understandable response.
3. Every number shown is real, live, correct.
4. Every toggle works immediately.
5. No fake data, no dead buttons, no emoji-as-design.
6. Overlays never close or block PES.
7. Clean battery/lifecycle idle + after exit.
8. One coherent visual system.
9. A user who plays one PES match says "that's useful, I'll keep it."

---

## F. DECISIONS TO LOCK (PES v1.0.1)

- [ ] The exact hero moment on PES launch (what the user sees).
- [ ] Dashboard content (which 5 stats are the core readout).
- [ ] Which features are core vs hidden/advanced.
- [ ] Free vs paid for this release.
- [ ] Design reference standard (a target app).
- [ ] Whether we commission a designer or execute a written spec.
- [ ] Language: English only, or English + Swahili?

---

## G. KEEPING THE FUTURE OPEN (no work, just awareness)
We won't build multi-game now, but these two things must NOT be hardcoded-to-PES
so the future is cheap:
- Game package name already in Constants (fine for now).
- VPN allow-list, detection, analytics should stay in one place so a future
  "profile" can reuse them. (No extra work now; just don't scatter PES constants
  further.)
