# Feature 8: Thermal Sentinel (Auto-throttle protection)

- **Category:** Perf
- **Implementation:** `utils/performance/ThermalUtils.kt`
- **Performance Confidence:** High

## Description

Reads CPU/battery temperature and switches to Eco mode or warns before MediaTek's 45C throttle kicks in, keeping clocks higher for longer.
