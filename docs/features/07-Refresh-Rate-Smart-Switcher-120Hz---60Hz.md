# Feature 7: Refresh Rate Smart Switcher (120Hz -> 60Hz)

- **Category:** Perf
- **Implementation:** `utils/performance/RefreshRateUtils.kt`
- **Performance Confidence:** High

## Description

Drops the display to 60Hz while gaming. The G81 GPU cannot push 120fps, so this halves GPU workload, reduces heat and micro-stutter.
