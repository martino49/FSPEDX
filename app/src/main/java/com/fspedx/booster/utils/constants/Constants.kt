package com.fspedx.booster.utils.constants

/**
 * Central constants file for FSPEDX.
 * Contains package names, SharedPreference keys, Intent actions, and static values.
 */
object Constants {

    // ============ PACKAGE NAMES ============
    /** eFootball 2024+ primary package name */
    const val PES_PACKAGE_NAME = "jp.konami.pesam"
    
    /** eFootball PES 2021 / legacy (some regions) */
    const val PES_PACKAGE_NAME_ALT = "com.konami.pes2019am"
    
    /** Additional known eFootball package variants */
    val PES_PACKAGE_NAMES = listOf(
        "jp.konami.pesam",       // eFootball 2024+
        "com.konami.pes2019am",  // PES 2019/2020 (older installs)
        "com.konami.pes",        // legacy
        "jp.konami.pes",         // legacy JP
        "com.konami.eFootball"
    )

    // ============ SHARED PREFERENCE KEYS ============
    const val PREF_NAME = "fspedx_prefs"
    
    // Onboarding
    const val KEY_FIRST_RUN = "first_run"
    const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    
    // Permissions status
    const val KEY_OVERLAY_GRANTED = "overlay_granted"
    const val KEY_ACCESSIBILITY_GRANTED = "accessibility_granted"
    const val KEY_VPN_GRANTED = "vpn_granted"
    const val KEY_DND_GRANTED = "dnd_granted"
    
    // Feature toggles (default = true for most)
    const val KEY_TURBO_MODE = "turbo_mode" // true = Turbo, false = Eco
    const val KEY_AUTO_LAUNCH = "auto_launch"
    const val KEY_AUTO_BOOST = "auto_boost"
    const val KEY_FPS_OVERLAY = "fps_overlay"
    const val KEY_REFRESH_RATE_SWITCH = "refresh_rate_switch"
    const val KEY_THERMAL_SENTINEL = "thermal_sentinel"
    const val KEY_WIFI_LOW_LATENCY = "wifi_low_latency"
    const val KEY_SMART_DNS = "smart_dns"
    const val KEY_DUAL_CHANNEL = "dual_channel"
    const val KEY_VPN_TUNNEL = "vpn_tunnel"
    const val KEY_FLOATING_PILL = "floating_pill"
    const val KEY_SIDE_PANEL = "side_panel"
    const val KEY_CROSSHAIR = "crosshair"
    const val KEY_DND_MODE = "dnd_mode"
    const val KEY_VIBRATION_TRAINING = "vibration_training"
    const val KEY_WHITELIST = "whitelist" // JSON array string
    
    // Feature #28: GPU rendering tuning
    const val KEY_GPU_FORCE_RENDERING = "gpu_force_rendering"
    const val KEY_GPU_DISABLE_OVERLAYS = "gpu_disable_overlays"

    // Feature #29: Gesture / edge blocking (sticky immersive + edge guard)
    const val KEY_EDGE_BLOCKING = "edge_blocking"

    // Feature: Keep screen on while gaming
    const val KEY_KEEP_SCREEN_ON = "keep_screen_on"

    // Feature: Interception banner (floating notifications/calls over game)
    const val KEY_INTERCEPTION_BANNER = "interception_banner"
    const val ACTION_DISMISS_BANNER = "com.fspedx.booster.DISMISS_BANNER"

    // Feature #30: Background data starvation
    const val KEY_BACKGROUND_DATA_STARVATION = "background_data_starvation"
    
    // Whitelist
    const val KEY_WHITELIST_APPS = "whitelist_apps" // Set<String>
    
    // Performance
    const val KEY_RAM_BOOST_TRIGGERED = "ram_boost_triggered"
    const val KEY_LAST_PING = "last_ping"
    const val KEY_LAST_PING_TIMESTAMP = "last_ping_timestamp"
    
    // Backup
    const val KEY_BACKUP_FILE_NAME = "fspedx_backup.json"

    // ============ INTENT ACTIONS ============
    const val ACTION_STOP_VPN = "com.fspedx.booster.STOP_VPN"
    const val ACTION_START_RECORDING = "com.fspedx.booster.START_RECORDING"
    const val ACTION_STOP_RECORDING = "com.fspedx.booster.STOP_RECORDING"
    const val ACTION_TOGGLE_PILL = "com.fspedx.booster.TOGGLE_PILL"
    const val ACTION_BOOST_NOW = "com.fspedx.booster.BOOST_NOW"
    const val ACTION_PING_TEST = "com.fspedx.booster.PING_TEST"

    // ============ NOTIFICATION CHANNEL IDs ============
    const val NOTIFICATION_CHANNEL_VPN = "fspedx_vpn_channel"
    const val NOTIFICATION_CHANNEL_RECORDING = "fspedx_recording_channel"
    const val NOTIFICATION_CHANNEL_OVERLAY = "fspedx_overlay_channel"
    const val NOTIFICATION_CHANNEL_DND = "fspedx_dnd_channel"
    
    // ============ NOTIFICATION IDs ============
    const val NOTIFICATION_ID_VPN = 1001
    const val NOTIFICATION_ID_RECORDING = 1002
    const val NOTIFICATION_ID_OVERLAY = 1003
    const val NOTIFICATION_ID_DND = 1004

    // ============ NETWORK CONSTANTS ============
    const val DNS_CLOUDFLARE = "1.1.1.1"
    const val DNS_CLOUDFLARE_ALT = "1.0.0.1"
    const val DNS_GOOGLE = "8.8.8.8"
    const val DNS_GOOGLE_ALT = "8.8.4.4"
    
    // VPN virtual IP
    const val VPN_ADDRESS = "10.0.0.2"
    const val VPN_PREFIX_LENGTH = 32
    
    // Ping timeout (milliseconds)
    const val PING_TIMEOUT_MS = 3000L
    const val PING_DEFAULT_HOST = "8.8.8.8"

    // ============ PERFORMANCE CONSTANTS ============
    const val TEMP_WARNING_THRESHOLD = 42.0  // Celsius
    const val TEMP_CRITICAL_THRESHOLD = 45.0 // Celsius
    const val MAX_RAM_GB = 4.0
    const val TARGET_RAM_PERCENT = 0.8 // 80%

    // ============ FILE PATHS ============
    const val RECORDINGS_DIR = "FSPEDX/Recordings"
    const val BACKUP_DIR = "FSPEDX/Backups"
    const val EXPORT_FILE_NAME = "fspedx_settings.json"

    // ============ DATABASE CONSTANTS ============
    const val DB_NAME = "fspedx_database"
    const val DB_VERSION = 1

    // ============ TIMING CONSTANTS ============
    const val OVERLAY_UPDATE_INTERVAL_MS = 1000L // 1 second
    const val AUTO_LAUNCH_CHECK_INTERVAL_MS = 2000L // 2 seconds
    const val SESSION_RECORDING_SEGMENT_MS = 5000L // 5 second chunks
}
