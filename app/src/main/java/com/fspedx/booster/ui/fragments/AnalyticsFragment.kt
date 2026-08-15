package com.fspedx.booster.ui.fragments

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.fspedx.booster.R
import com.fspedx.booster.data.models.TouchEvent
import com.fspedx.booster.utils.ml.KMeansCluster
import com.fspedx.booster.viewmodels.AnalyticsViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class AnalyticsFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.analyticsViewPager)
        tabLayout = view.findViewById(R.id.analyticsTabLayout)

        // Set up ViewPager with 3 tabs
        val adapter = AnalyticsPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Heatmap"
                1 -> tab.text = "Mis-taps"
                2 -> tab.text = "Timeline"
            }
        }.attach()
    }

    // ===== INNER FRAGMENTS FOR TABS =====

    class HeatmapFragment : Fragment() {
        private val viewModel: AnalyticsViewModel by viewModels()

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.fragment_heatmap, container, false)

            val imageView = view.findViewById<ImageView>(R.id.heatmapImageView)
            val tvCentroids = view.findViewById<TextView>(R.id.tvCentroids)
            val btnSuggest = view.findViewById<android.widget.Button>(R.id.btnSuggestPositions)

            // Load real touch data from the database
            viewModel.loadTouchEvents()

            btnSuggest.setOnClickListener {
                btnSuggest.isEnabled = false
                viewModel.findButtonCentroids(4) // Pass, Shoot, Sprint, Direction
                viewModel.detectMisTaps()
                btnSuggest.isEnabled = true
                val centroids = viewModel.centroids.value
                if (centroids.isEmpty()) {
                    tvCentroids.text = "Not enough touch data yet. Play a match with the heatmap recorder on."
                } else {
                    tvCentroids.text = "Suggested positions:\n" +
                        centroids.joinToString("\n") { "X: ${it.first.toInt()}, Y: ${it.second.toInt()}" }
                }
            }

            // Observe touch data and draw the heatmap overlay on the controller image
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.touchEvents.collect { events ->
                    drawHeatmap(imageView, events)
                }
            }

            return view
        }

        /**
         * Draws semi-transparent dots over the controller image for each touch
         * point, plus cyan markers for the computed centroids.
         */
        private fun drawHeatmap(imageView: ImageView, events: List<TouchEvent>) {
            if (events.isEmpty()) {
                imageView.setImageResource(R.drawable.default_controller)
                return
            }

            val base = android.graphics.BitmapFactory.decodeResource(
                resources,
                R.drawable.default_controller
            )?.copy(android.graphics.Bitmap.Config.ARGB_8888, true) ?: return

            val canvas = Canvas(base)
            val w = base.width.toFloat()
            val h = base.height.toFloat()

            // Touch dots (heat)
            val touchPaint = Paint().apply {
                isAntiAlias = true
                color = 0x8800F0FF.toInt() // translucent cyan
                style = Paint.Style.FILL
            }
            for (e in events) {
                // Normalize touch screen coords onto the controller image
                val px = (e.x / 1080f) * w
                val py = (e.y / 2340f) * h
                canvas.drawCircle(px, py, 12f, touchPaint)
            }

            // Centroid markers
            val centroidPaint = Paint().apply {
                isAntiAlias = true
                color = 0xFFFF003C.toInt() // danger red
                style = Paint.Style.FILL
            }
            for (c in viewModel.centroids.value) {
                val px = (c.first / 1080f) * w
                val py = (c.second / 2340f) * h
                canvas.drawCircle(px, py, 16f, centroidPaint)
            }

            imageView.setImageBitmap(base)
        }
    }

    class MisTapFragment : Fragment() {
        private val viewModel: AnalyticsViewModel by viewModels()

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.fragment_mistaps, container, false)

            val tvMisTaps = view.findViewById<TextView>(R.id.tvMisTapCount)
            val btnTrain = view.findViewById<android.widget.Button>(R.id.btnVibrationTraining)

            // Load and display real mis-tap data
            viewModel.loadTouchEvents()
            btnTrain.setOnClickListener {
                viewModel.findButtonCentroids(4)
                viewModel.detectMisTaps()
                val count = viewModel.misTapCount.value
                tvMisTaps.text = "Mis-taps detected: $count"
            }

            // Observe mis-tap count updates automatically
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.misTapCount.collect { count ->
                    tvMisTaps.text = "Mis-taps detected: $count"
                }
            }

            // Vibration training toggle (persisted)
            val prefs = requireContext().getSharedPreferences(
                com.fspedx.booster.utils.constants.Constants.PREF_NAME,
                android.content.Context.MODE_PRIVATE
            )
            val isTraining = prefs.getBoolean(
                com.fspedx.booster.utils.constants.Constants.KEY_VIBRATION_TRAINING,
                false
            )
            btnTrain.text = if (isTraining) "Vibration Training: ON" else "Vibration Training: OFF"
            btnTrain.setOnLongClickListener {
                val newState = !prefs.getBoolean(
                    com.fspedx.booster.utils.constants.Constants.KEY_VIBRATION_TRAINING,
                    false
                )
                prefs.edit()
                    .putBoolean(com.fspedx.booster.utils.constants.Constants.KEY_VIBRATION_TRAINING, newState)
                    .apply()
                btnTrain.text = if (newState) "Vibration Training: ON" else "Vibration Training: OFF"
                android.widget.Toast.makeText(
                    requireContext(),
                    if (newState) "Vibration training enabled" else "Vibration training disabled",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                true
            }

            return view
        }
    }

    class TimelineFragment : Fragment() {
        private val viewModel: AnalyticsViewModel by viewModels()

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.fragment_timeline, container, false)

            val lineChart = view.findViewById<LineChart>(R.id.lagTimelineChart)

            // Load real lag spikes from the database
            viewModel.loadLagSpikes()

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.lagSpikes.collect { spikes ->
                    updateChart(lineChart, spikes)
                }
            }

            return view
        }

        private fun updateChart(lineChart: LineChart, spikes: List<com.fspedx.booster.data.models.LagSpike>) {
            if (spikes.isEmpty()) {
                lineChart.data = null
                lineChart.clear()
                lineChart.invalidate()
                return
            }

            val entries = spikes.mapIndexed { index, spike ->
                Entry(index.toFloat(), spike.ping.toFloat())
            }

            val dataSet = LineDataSet(entries, "Ping (ms)")
            dataSet.color = resources.getColor(R.color.primary_cyan, null)
            dataSet.setCircleColor(resources.getColor(R.color.primary_cyan, null))
            dataSet.lineWidth = 2f
            dataSet.circleRadius = 3f
            dataSet.valueTextColor = resources.getColor(R.color.text_secondary, null)
            dataSet.setDrawFilled(true)
            dataSet.fillColor = resources.getColor(R.color.primary_cyan, null)
            dataSet.fillAlpha = 60

            val lineData = LineData(dataSet)
            lineChart.data = lineData

            lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            lineChart.axisRight.isEnabled = false
            lineChart.description.isEnabled = false
            lineChart.setPinchZoom(true)
            lineChart.animateX(800)
            lineChart.invalidate()
        }
    }

    // ===== VIEWPAGER ADAPTER =====

    inner class AnalyticsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HeatmapFragment()
                1 -> MisTapFragment()
                2 -> TimelineFragment()
                else -> HeatmapFragment()
            }
        }
    }
}
