# Feature 10: Auto-Launch Trigger (Detects PES start)

- **Category:** Perf
- **Implementation:** `FSPEDXApplication.kt`
- **Performance Confidence:** High

## Description

Polling loop using UsageStatsManager detects when PES comes to the foreground and automatically applies the enabled boosters.
