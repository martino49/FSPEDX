# Feature 13: Smart DNS (Cloudflare/Google)

- **Category:** Network
- **Implementation:** `services/GameVpnService.kt`
- **Performance Confidence:** Medium

## Description

Routes DNS through fast resolvers (1.1.1.1 / 8.8.8.8) to cut matchmaking/search latency by 10-30ms.
