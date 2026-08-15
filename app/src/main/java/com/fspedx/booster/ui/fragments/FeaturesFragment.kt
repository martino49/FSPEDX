package com.fspedx.booster.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import com.fspedx.booster.FSPEDXApplication
import com.fspedx.booster.R
import com.fspedx.booster.services.GameVpnService
import com.fspedx.booster.services.OverlayService
import com.fspedx.booster.services.SessionRecorderService
import com.fspedx.booster.utils.backup.BackupManager
import com.fspedx.booster.utils.constants.Constants
import com.fspedx.booster.utils.performance.RefreshRateUtils
import com.fspedx.booster.utils.performance.ThermalUtils
import com.google.android.material.switchmaterial.SwitchMaterial
import java.io.File

class FeaturesFragment : Fragment() {

    // UI elements
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FeatureAdapter

    private val prefs by lazy { (requireContext().applicationContext as FSPEDXApplication).prefs }

    // File picker for importing a backup
    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        importBackup(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_features, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.featuresRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Build feature list
        val features = buildFeatureList()
        adapter = FeatureAdapter(features)
        recyclerView.adapter = adapter
    }

    private fun buildFeatureList(): List<FeatureItem> {
        return listOf(
            // ===== SECTION A: PERFORMANCE ENGINE =====
            FeatureItem.Section("PERFORMANCE ENGINE"),
            FeatureItem.Toggle(
                "FPS / Temp Overlay",
                "Show live FPS and battery temperature",
                R.drawable.ic_speed,
                prefs.getBoolean(Constants.KEY_FPS_OVERLAY, true),
                Constants.KEY_FPS_OVERLAY
            ),
            FeatureItem.Toggle(
                "Refresh Rate Switch",
                "Drop 120Hz → 60Hz for stable gaming",
                R.drawable.ic_refresh,
                prefs.getBoolean(Constants.KEY_REFRESH_RATE_SWITCH, true),
                Constants.KEY_REFRESH_RATE_SWITCH
            ),
            FeatureItem.Toggle(
                "Thermal Sentinel",
                "Auto-throttle if temp > 45°C",
                R.drawable.ic_thermostat,
                prefs.getBoolean(Constants.KEY_THERMAL_SENTINEL, true),
                Constants.KEY_THERMAL_SENTINEL
            ),
            FeatureItem.Toggle(
                "Force GPU Rendering",
                "Offload drawing to the GPU (feature #28)",
                R.drawable.ic_boost,
                prefs.getBoolean(Constants.KEY_GPU_FORCE_RENDERING, false),
                Constants.KEY_GPU_FORCE_RENDERING
            ),
            FeatureItem.Toggle(
                "Disable HW Overlays",
                "Force GPU composition (feature #28)",
                R.drawable.ic_visibility,
                prefs.getBoolean(Constants.KEY_GPU_DISABLE_OVERLAYS, false),
                Constants.KEY_GPU_DISABLE_OVERLAYS
            ),
            FeatureItem.Toggle(
                "Background Data Starvation",
                "Block app sync while PES is active (feature #30)",
                R.drawable.ic_signal_cellular_alt,
                prefs.getBoolean(Constants.KEY_BACKGROUND_DATA_STARVATION, false),
                Constants.KEY_BACKGROUND_DATA_STARVATION
            ),
            FeatureItem.RadioGroup(
                "Profile Selector",
                listOf("Turbo", "Eco"),
                if (prefs.getBoolean(Constants.KEY_TURBO_MODE, true)) 0 else 1
            ),
            FeatureItem.Button(
                "Safe Cache Cleaner",
                "Clear junk from other apps (skips PES)",
                R.drawable.ic_delete_sweep
            ) { cleanCache() },

            // ===== SECTION B: NETWORK TUNNELING =====
            FeatureItem.Section("NETWORK TUNNELING"),
            FeatureItem.Toggle(
                "Wi-Fi Low-Latency Lock",
                "Prioritize game packets over battery",
                R.drawable.ic_wifi,
                prefs.getBoolean(Constants.KEY_WIFI_LOW_LATENCY, true),
                Constants.KEY_WIFI_LOW_LATENCY
            ),
            FeatureItem.Toggle(
                "Smart DNS",
                "Use Cloudflare (1.1.1.1) for faster lookups",
                R.drawable.ic_dns,
                prefs.getBoolean(Constants.KEY_SMART_DNS, true),
                Constants.KEY_SMART_DNS
            ),
            FeatureItem.Toggle(
                "Dual-Channel Rescue",
                "Use WiFi + Mobile Data simultaneously",
                R.drawable.ic_signal_cellular_alt,
                prefs.getBoolean(Constants.KEY_DUAL_CHANNEL, false),
                Constants.KEY_DUAL_CHANNEL
            ),
            FeatureItem.Toggle(
                "Local VPN Tunnel",
                "Route only PES traffic through our tunnel",
                R.drawable.ic_vpn_lock,
                prefs.getBoolean(Constants.KEY_VPN_TUNNEL, true),
                Constants.KEY_VPN_TUNNEL
            ),
            FeatureItem.Button(
                "Pre-Match Radar",
                "Run a quick ping test to Konami's servers",
                R.drawable.ic_radar
            ) { runPingTest() },

            // ===== SECTION C: IN-GAME OVERLAYS =====
            FeatureItem.Section("IN-GAME OVERLAYS"),
            FeatureItem.Toggle(
                "Floating Quick-Pill",
                "Access controls without leaving your game",
                R.drawable.ic_gesture_pill,
                prefs.getBoolean(Constants.KEY_FLOATING_PILL, true),
                Constants.KEY_FLOATING_PILL
            ),
            FeatureItem.Toggle(
                "Side Pull-Out Table",
                "Swipe from left for live stats dashboard",
                R.drawable.ic_table_rows,
                prefs.getBoolean(Constants.KEY_SIDE_PANEL, true),
                Constants.KEY_SIDE_PANEL
            ),
            FeatureItem.Toggle(
                "Aiming Crosshair",
                "Drag an overlay guide for precise passes/shots",
                R.drawable.ic_crosshair,
                prefs.getBoolean(Constants.KEY_CROSSHAIR, false),
                Constants.KEY_CROSSHAIR
            ),
            FeatureItem.Toggle(
                "Gesture / Edge Blocking",
                "Stop accidental edge-swipe back while gaming (feature #29)",
                R.drawable.ic_gesture_pill,
                prefs.getBoolean(Constants.KEY_EDGE_BLOCKING, false),
                Constants.KEY_EDGE_BLOCKING
            ),

            // ===== SECTION D: SYSTEM & PROTECTION =====
            FeatureItem.Section("SYSTEM & PROTECTION"),
            FeatureItem.Toggle(
                "Auto-Launch Trigger",
                "Auto-enable boosters when PES starts",
                R.drawable.ic_play_circle,
                prefs.getBoolean(Constants.KEY_AUTO_LAUNCH, true),
                Constants.KEY_AUTO_LAUNCH
            ),
            FeatureItem.Toggle(
                "Smart DND",
                "Mute calls and notifications during matches",
                R.drawable.ic_volume_off,
                prefs.getBoolean(Constants.KEY_DND_MODE, true),
                Constants.KEY_DND_MODE
            ),
            FeatureItem.Toggle(
                "Keep Screen On",
                "Screen never sleeps while PES is running",
                R.drawable.ic_visibility,
                prefs.getBoolean(Constants.KEY_KEEP_SCREEN_ON, false),
                Constants.KEY_KEEP_SCREEN_ON
            ),
            FeatureItem.Toggle(
                "Floating Notifications",
                "Show messages as a floating banner over the game",
                R.drawable.ic_touch_app,
                prefs.getBoolean(Constants.KEY_INTERCEPTION_BANNER, true),
                Constants.KEY_INTERCEPTION_BANNER
            ),
            FeatureItem.Button(
                "App Whitelist",
                "Protect apps from being killed by RAM booster",
                R.drawable.ic_shield
            ) { showWhitelistDialog() },
            FeatureItem.Button(
                "Backup & Restore",
                "Export/Import settings and heatmap data",
                R.drawable.ic_backup
            ) { showBackupDialog() },
        )
    }

    // ============ ACTION HANDLERS ============

    private fun cleanCache() {
        Toast.makeText(requireContext(), "Cleaning cache...", Toast.LENGTH_SHORT).show()
        // Run on a background thread to avoid jank; skip PES packages.
        Thread {
            val context = requireContext()
            // Real implementation: iterate installed apps, skip PES + system,
            // and clear each app's cacheDir via PackageManager.getApplicationInfo.
            val pm = context.packageManager
            val pesPackages = Constants.PES_PACKAGE_NAMES
            var cleaned = 0L
            try {
                val installed = pm.getInstalledApplications(0)
                for (app in installed) {
                    if (app.packageName in pesPackages) continue
                    if (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0) continue
                    if (app.packageName == context.packageName) continue
                    try {
                        val cacheDir = app.dataDir?.let { java.io.File(it, "cache") }
                        cleaned += deleteRecursive(cacheDir)
                    } catch (e: Exception) {
                        // Skip apps we can't access
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
            val mb = cleaned / (1024 * 1024)
            requireActivity().runOnUiThread {
                Toast.makeText(
                    context,
                    if (mb > 0) "Cache cleaned: $mb MB freed" else "No cache to clean",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.start()
    }

    private fun deleteRecursive(file: java.io.File?): Long {
        if (file == null || !file.exists()) return 0L
        var size = 0L
        if (file.isDirectory) {
            file.listFiles()?.forEach { size += deleteRecursive(it) }
        } else {
            size = file.length()
        }
        file.delete()
        return size
    }

    private fun runPingTest() {
        Toast.makeText(requireContext(), "Running ping test...", Toast.LENGTH_SHORT).show()
        // Delegate to the network utilities on a background thread (non-suspend wrapper)
        Thread {
            val start = System.currentTimeMillis()
            val reachable = try {
                java.net.InetAddress.getByName(Constants.PING_DEFAULT_HOST)
                    .isReachable(Constants.PING_TIMEOUT_MS.toInt())
            } catch (e: Exception) {
                false
            }
            val ping = if (reachable) (System.currentTimeMillis() - start).toInt() else -1
            requireActivity().runOnUiThread {
                if (ping > 0) {
                    Toast.makeText(requireContext(), "Ping: $ping ms", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Ping test failed", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun showWhitelistDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_whitelist, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.whitelistRecyclerView)
        val tvEmpty = dialogView.findViewById<android.widget.TextView>(R.id.tvWhitelistEmpty)

        val whitelist = (prefs.getStringSet(Constants.KEY_WHITELIST_APPS, emptySet())
            ?: emptySet()).toMutableSet()

        var adapter: com.fspedx.booster.ui.adapters.WhitelistAdapter? = null
        adapter = com.fspedx.booster.ui.adapters.WhitelistAdapter(
            requireContext(),
            whitelist
        ) { pkg ->
            whitelist.remove(pkg)
            prefs.edit().putStringSet(Constants.KEY_WHITELIST_APPS, whitelist).apply()
            adapter?.notifyDataSetChanged()
            tvEmpty.visibility = if (whitelist.isEmpty()) View.VISIBLE else View.GONE
            Toast.makeText(requireContext(), "Removed from whitelist", Toast.LENGTH_SHORT).show()
        }

        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        tvEmpty.visibility = if (whitelist.isEmpty()) View.VISIBLE else View.GONE

        AlertDialog.Builder(requireContext())
            .setTitle("Protected Apps")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                // Open system app picker to choose an app to protect
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                startActivity(intent)
                Toast.makeText(
                    requireContext(),
                    "Whitelist entries persist; use the installed-apps list in your launcher.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showBackupDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Backup & Restore")
            .setMessage("Export settings to file?")
            .setPositiveButton("Export") { _, _ ->
                lifecycleScope.launch {
                    Toast.makeText(requireContext(), "Exporting...", Toast.LENGTH_SHORT).show()
                    val success = BackupManager.exportSettings(requireContext())
                    Toast.makeText(
                        requireContext(),
                        if (success) "Settings exported to Downloads" else "Export failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Import", { _, _ ->
                importBackupLauncher.launch(arrayOf("application/json"))
            })
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun importBackup(uri: Uri) {
        Toast.makeText(requireContext(), "Importing backup...", Toast.LENGTH_SHORT).show()
        // Copy the selected document into app storage so BackupManager can read it,
        // then import on a coroutine (BackupManager methods are suspend).
        lifecycleScope.launch {
            try {
                val tempFile = File(requireContext().cacheDir, "import_backup.json")
                requireContext().contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                val success = BackupManager.importSettings(requireContext(), tempFile.absolutePath)
                Toast.makeText(
                    requireContext(),
                    if (success) "Backup restored" else "Import failed",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Import failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ============ TOGGLE LISTENER ============

    inner class FeatureAdapter(private val items: List<FeatureItem>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_SECTION = 0
        private val TYPE_TOGGLE = 1
        private val TYPE_RADIO = 2
        private val TYPE_BUTTON = 3

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is FeatureItem.Section -> TYPE_SECTION
                is FeatureItem.Toggle -> TYPE_TOGGLE
                is FeatureItem.RadioGroup -> TYPE_RADIO
                is FeatureItem.Button -> TYPE_BUTTON
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                TYPE_SECTION -> {
                    val view = inflater.inflate(R.layout.item_feature_section, parent, false)
                    SectionViewHolder(view)
                }
                TYPE_TOGGLE -> {
                    val view = inflater.inflate(R.layout.item_feature_toggle, parent, false)
                    ToggleViewHolder(view)
                }
                TYPE_RADIO -> {
                    val view = inflater.inflate(R.layout.item_feature_radio, parent, false)
                    RadioViewHolder(view)
                }
                else -> {
                    val view = inflater.inflate(R.layout.item_feature_button, parent, false)
                    ButtonViewHolder(view)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is SectionViewHolder -> {
                    val item = items[position] as FeatureItem.Section
                    holder.bind(item)
                }
                is ToggleViewHolder -> {
                    val item = items[position] as FeatureItem.Toggle
                    holder.bind(item)
                }
                is RadioViewHolder -> {
                    val item = items[position] as FeatureItem.RadioGroup
                    holder.bind(item)
                }
                is ButtonViewHolder -> {
                    val item = items[position] as FeatureItem.Button
                    holder.bind(item)
                }
            }
        }

        override fun getItemCount(): Int = items.size

        // ===== VIEW HOLDERS =====

        inner class SectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvSection = view.findViewById<android.widget.TextView>(R.id.tvSectionTitle)

            fun bind(item: FeatureItem.Section) {
                tvSection.text = item.title
            }
        }

        inner class ToggleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvTitle = view.findViewById<android.widget.TextView>(R.id.tvToggleTitle)
            private val tvDesc = view.findViewById<android.widget.TextView>(R.id.tvToggleDesc)
            private val switch = view.findViewById<SwitchMaterial>(R.id.switchToggle)

            fun bind(item: FeatureItem.Toggle) {
                tvTitle.text = item.title
                tvDesc.text = item.description
                switch.isChecked = item.isChecked
                switch.setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit().putBoolean(item.prefKey, isChecked).apply()
                    // Handle specific toggle actions
                    when (item.prefKey) {
                        Constants.KEY_FPS_OVERLAY -> {
                            if (isChecked) OverlayService.start(requireContext())
                            else OverlayService.stop(requireContext())
                        }
                        Constants.KEY_VPN_TUNNEL -> {
                            if (isChecked) {
                                val intent = android.net.VpnService.prepare(requireContext())
                                if (intent != null) {
                                    startActivity(intent)
                                } else {
                                    GameVpnService.start(requireContext())
                                }
                            } else {
                                GameVpnService.stop(requireContext())
                            }
                        }
                        Constants.KEY_REFRESH_RATE_SWITCH -> {
                            if (isChecked) {
                                if (!RefreshRateUtils.canWriteSettings(requireContext())) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Grant 'Modify system settings' to use Refresh Rate Switch",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    com.fspedx.booster.utils.permissions.PermissionHelper
                                        .requestWriteSettingsPermission(requireActivity())
                                } else {
                                    RefreshRateUtils.setRefreshRate(requireContext(), 60)
                                }
                            } else {
                                RefreshRateUtils.setRefreshRate(requireContext(), 120)
                            }
                        }
                        Constants.KEY_FLOATING_PILL -> {
                            if (isChecked) OverlayService.showFloatingPill(requireContext())
                            else OverlayService.hideFloatingPill(requireContext())
                        }
                        Constants.KEY_AUTO_LAUNCH -> {
                            (requireActivity().application as FSPEDXApplication)
                                .applyAutoLaunchPreference(isChecked)
                        }
                        Constants.KEY_BACKGROUND_DATA_STARVATION -> {
                            val app = requireActivity().application as FSPEDXApplication
                            com.fspedx.booster.utils.network.DataGovernor
                                .evaluate(requireContext(), app.isPesForeground.value)
                        }
                        Constants.KEY_EDGE_BLOCKING -> {
                            OverlayService.setEdgeBlocking(isChecked)
                        }
                        Constants.KEY_GPU_FORCE_RENDERING -> {
                            if (isChecked && !com.fspedx.booster.utils.permissions.PermissionHelper
                                    .hasWriteSettingsPermission(requireContext())
                            ) {
                                Toast.makeText(
                                    requireContext(),
                                    "Grant 'Modify system settings' to use GPU tuning",
                                    Toast.LENGTH_LONG
                                ).show()
                                com.fspedx.booster.utils.permissions.PermissionHelper
                                    .requestWriteSettingsPermission(requireActivity())
                            } else {
                                com.fspedx.booster.utils.performance.PerformanceTuning
                                    .setForceGpuRendering(requireContext(), isChecked)
                            }
                        }
                        Constants.KEY_GPU_DISABLE_OVERLAYS -> {
                            if (isChecked && !com.fspedx.booster.utils.permissions.PermissionHelper
                                    .hasWriteSettingsPermission(requireContext())
                            ) {
                                Toast.makeText(
                                    requireContext(),
                                    "Grant 'Modify system settings' to use GPU tuning",
                                    Toast.LENGTH_LONG
                                ).show()
                                com.fspedx.booster.utils.permissions.PermissionHelper
                                    .requestWriteSettingsPermission(requireActivity())
                            } else {
                                com.fspedx.booster.utils.performance.PerformanceTuning
                                    .setDisableHwOverlays(requireContext(), isChecked)
                            }
                        }
                        // Add more toggle handlers as needed
                    }
                    Toast.makeText(requireContext(), "${item.title}: ${if (isChecked) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        inner class RadioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvTitle = view.findViewById<android.widget.TextView>(R.id.tvRadioTitle)
            private val radioGroup = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.radioGroup)

            fun bind(item: FeatureItem.RadioGroup) {
                tvTitle.text = item.title
                // Set selection
                when (item.defaultSelected) {
                    0 -> radioGroup.check(R.id.radioTurbo)
                    1 -> radioGroup.check(R.id.radioEco)
                }
                radioGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
                    if (isChecked) {
                        val isTurbo = checkedId == R.id.radioTurbo
                        prefs.edit().putBoolean(Constants.KEY_TURBO_MODE, isTurbo).apply()
                        Toast.makeText(requireContext(), "Profile: ${if (isTurbo) "Turbo" else "Eco"}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        inner class ButtonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val btn = view.findViewById<android.widget.Button>(R.id.btnFeatureAction)
            private val tvDesc = view.findViewById<android.widget.TextView>(R.id.tvButtonDesc)

            fun bind(item: FeatureItem.Button) {
                btn.text = item.title
                tvDesc.text = item.description
                btn.setOnClickListener { item.action.invoke() }
            }
        }
    }
}

// ============ DATA CLASSES FOR FEATURE ITEMS ============

sealed class FeatureItem {
    data class Section(val title: String) : FeatureItem()
    data class Toggle(
        val title: String,
        val description: String,
        val iconRes: Int,
        val isChecked: Boolean,
        val prefKey: String
    ) : FeatureItem()

    data class RadioGroup(
        val title: String,
        val options: List<String>,
        val defaultSelected: Int
    ) : FeatureItem()

    data class Button(
        val title: String,
        val description: String,
        val iconRes: Int,
        val action: () -> Unit
    ) : FeatureItem()
}
