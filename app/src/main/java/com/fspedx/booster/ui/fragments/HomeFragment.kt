package com.fspedx.booster.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.fspedx.booster.FSPEDXApplication
import com.fspedx.booster.R
import com.fspedx.booster.services.SessionRecorderService
import com.fspedx.booster.utils.constants.Constants
import com.fspedx.booster.utils.performance.RamCleaner
import com.fspedx.booster.utils.performance.ThermalUtils
import com.fspedx.booster.viewmodels.MainViewModel

class HomeFragment : Fragment() {

    private lateinit var viewModel: MainViewModel

    // UI Elements
    private lateinit var tvBattery: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvPing: TextView
    private lateinit var tvRam: TextView
    private lateinit var progressRam: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var ivStatus: android.widget.ImageView
    private lateinit var tvNetwork: TextView
    private lateinit var tvBatteryTile: TextView
    private lateinit var tvTempTile: TextView
    private lateinit var tvPingTile: TextView
    private lateinit var btnBoost: Button
    private lateinit var btnRadar: Button
    private lateinit var btnRecord: Button

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isRecording = false
    private var updateRunnable: Runnable? = null

    // MediaProjection permission launcher for screen recording
    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            // Configure the projection token, then start recording
            SessionRecorderService.setMediaProjection(result.resultCode, result.data!!)
            SessionRecorderService.start(requireContext())
            isRecording = true
            updateRecordUi()
        } else {
            android.widget.Toast.makeText(
                requireContext(),
                "Screen recording permission denied",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init ViewModel
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Bind UI
        tvBattery = view.findViewById(R.id.tvBattery)
        tvTemp = view.findViewById(R.id.tvTemp)
        tvPing = view.findViewById(R.id.tvPing)
        tvRam = view.findViewById(R.id.tvRam)
        progressRam = view.findViewById(R.id.progressRam)
        tvStatus = view.findViewById(R.id.tvStatus)
        ivStatus = view.findViewById(R.id.ivStatus)
        tvNetwork = view.findViewById(R.id.tvNetwork)
        tvBatteryTile = view.findViewById(R.id.tvBatteryTile)
        tvTempTile = view.findViewById(R.id.tvTempTile)
        tvPingTile = view.findViewById(R.id.tvPingTile)
        btnBoost = view.findViewById(R.id.btnBoost)
        btnRadar = view.findViewById(R.id.btnRadar)
        btnRecord = view.findViewById(R.id.btnRecord)

        // Set max for RAM progress bar (4GB)
        progressRam.max = (Constants.MAX_RAM_GB * 1024).toInt()

        // Boost button
        btnBoost.setOnClickListener {
            performBoost()
        }

        // Radar (Ping test) button
        btnRadar.setOnClickListener {
            performPingTest()
        }

        // Record button
        btnRecord.setOnClickListener {
            toggleRecording()
        }

        // Start live updates
        startLiveUpdates()
    }

    private fun performBoost() {
        // Show immediate feedback
        btnBoost.isEnabled = false
        btnBoost.text = getString(R.string.home_boost_loading)

        // Run in background
        Thread {
            val prefs = (requireContext().applicationContext as FSPEDXApplication).prefs
            val whitelist = prefs.getStringSet(Constants.KEY_WHITELIST_APPS, emptySet()) ?: emptySet()
            val freedMB = RamCleaner.forceClean(requireContext(), whitelist)

            // Update UI on main thread
            requireActivity().runOnUiThread {
                btnBoost.isEnabled = true
                btnBoost.text = getString(R.string.home_boost)
                // Show toast or snackbar
                android.widget.Toast.makeText(
                    requireContext(),
                    "Freed $freedMB MB RAM",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                // Refresh RAM display
                updateStats()
            }
        }.start()
    }

    private fun performPingTest() {
        btnRadar.isEnabled = false
        btnRadar.text = getString(R.string.home_radar_loading)

        // Delegate to the ViewModel (runs on a coroutine, not a raw thread)
        viewModel.runPingTest()
        btnRadar.postDelayed({
            btnRadar.isEnabled = true
            btnRadar.text = getString(R.string.home_radar)
            val ping = viewModel.lastPing.value
            if (ping != null && ping > 0) {
                tvPing.text = "$ping ms"
                // Update status color + icon
                when {
                    ping < 50 -> setStatus("Excellent", R.color.success_green, R.drawable.ic_check_circle)
                    ping < 150 -> setStatus("Good", R.color.warning_orange, R.drawable.ic_info)
                    else -> setStatus("Poor", R.color.danger_red, R.drawable.ic_warning)
                }
                val message = if (ping < 50) "Excellent network!"
                               else if (ping < 150) "Good network."
                               else "High ping! Consider enabling Dual-Channel Rescue."
                android.widget.Toast.makeText(requireContext(), "$ping ms - $message", android.widget.Toast.LENGTH_LONG).show()
            } else {
                android.widget.Toast.makeText(requireContext(), "Ping test failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }, 3500)
    }

    private fun toggleRecording() {
        if (isRecording) {
            // Stop recording
            SessionRecorderService.stop(requireContext())
            isRecording = false
            updateRecordUi()
            android.widget.Toast.makeText(requireContext(), "Recording stopped & saved", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            // Request MediaProjection permission first
            val projectionManager = requireContext().getSystemService(
                android.content.Context.MEDIA_PROJECTION_SERVICE
            ) as android.media.projection.MediaProjectionManager
            mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        }
    }

    private fun updateRecordUi() {
        if (isRecording) {
            btnRecord.text = "STOP"
            btnRecord.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.danger_red))
        } else {
            btnRecord.text = "RECORD"
            btnRecord.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_cyan))
        }
    }

    private fun startLiveUpdates() {
        updateRunnable = object : Runnable {
            override fun run() {
                updateStats()
                mainHandler.postDelayed(this, Constants.OVERLAY_UPDATE_INTERVAL_MS)
            }
        }
        updateRunnable?.let { mainHandler.post(it) }
    }

    private fun updateStats() {
        // Get battery level
        val batteryLevel = ThermalUtils.getBatteryLevel(requireContext())
        tvBattery.text = "$batteryLevel%"
        tvBatteryTile.text = "$batteryLevel%"

        // Get temperature
        val temp = ThermalUtils.getBatteryTemperature(requireContext())
        tvTemp.text = "${temp.toInt()}°C"
        tvTempTile.text = "${temp.toInt()}°C"

        // Get RAM usage
        val ramInfo = RamCleaner.getRamUsage(requireContext())
        val usedMB = ramInfo.first
        val totalMB = ramInfo.second
        tvRam.text = "$usedMB / $totalMB MB"
        progressRam.progress = usedMB

        // Network type
        tvNetwork.text = com.fspedx.booster.utils.network.NetworkUtils.getNetworkType(requireContext())

        // Get ping (from ViewModel or last known)
        val ping = viewModel.lastPing.value ?: 0
        if (ping > 0) {
            tvPing.text = "$ping ms"
            tvPingTile.text = "$ping ms"
        }

        // Update status based on ping
        val currentPing = tvPing.text.toString().replace(" ms", "").toIntOrNull() ?: 0
        when {
            currentPing in 1..50 -> setStatus("Excellent", R.color.success_green, R.drawable.ic_check_circle)
            currentPing in 51..150 -> setStatus("Good", R.color.warning_orange, R.drawable.ic_info)
            currentPing > 150 -> setStatus("Poor", R.color.danger_red, R.drawable.ic_warning)
            else -> setStatus("Ready", R.color.success_green, R.drawable.ic_ready)
        }
    }

    /**
     * Sets the status text + icon + color together.
     */
    private fun setStatus(text: String, colorRes: Int, iconRes: Int) {
        tvStatus.text = text
        val color = ContextCompat.getColor(requireContext(), colorRes)
        tvStatus.setTextColor(color)
        ivStatus.setColorFilter(color)
        ivStatus.setImageResource(iconRes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updateRunnable?.let { mainHandler.removeCallbacks(it) }
    }
}
