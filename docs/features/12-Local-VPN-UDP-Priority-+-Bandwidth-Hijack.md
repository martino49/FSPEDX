# Feature 12: Local VPN (UDP Priority + Bandwidth Hijack)

- **Category:** Network
- **Implementation:** `services/GameVpnService.kt`
- **Performance Confidence:** High

## Description

Local VPN tunnel that writes UDP game packets to the socket immediately while TCP (non-game) traffic waits, reducing input lag for passes/shots.
