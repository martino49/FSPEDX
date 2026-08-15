# Feature 2: 80% Priority Lock + Force-Stop Background Apps

- **Category:** RAM
- **Implementation:** `utils/performance/RamCleaner.kt`
- **Performance Confidence:** Medium

## Description

Frees RAM by force-stopping non-whitelisted background apps so the system stops swapping memory to storage (which is slow). Reduces stutter and prevents 'app killed' crashes.
