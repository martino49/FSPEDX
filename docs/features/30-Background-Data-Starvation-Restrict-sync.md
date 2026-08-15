# Feature 30: Background Data Starvation (Restrict sync)

- **Category:** Perf
- **Implementation:** `services/GameVpnService.kt (to add)`
- **Performance Confidence:** High

## Description

NetworkPolicyManager restricts background data for all apps except PES while the game runs, stopping CPU wake-ups from WhatsApp/Instagram sync.
