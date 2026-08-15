# Feature 28: GPU Rendering Tuning (Force GPU + Disable HW Overlays)

- **Category:** Perf
- **Implementation:** `utils/performance (to add)`
- **Performance Confidence:** High

## Description

Writes Settings.Global for force_gpu_rendering and disable_overlays so the GPU (not CPU) handles composition, cutting CPU load 15-20%. Requires WRITE_SETTINGS.
