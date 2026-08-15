package com.fspedx.booster.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fspedx.booster.FSPEDXApplication
import com.fspedx.booster.data.database.AppDatabase
import com.fspedx.booster.data.models.LagSpike
import com.fspedx.booster.data.models.TouchEvent
import com.fspedx.booster.utils.ml.KMeansCluster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnalyticsViewModel : ViewModel() {

    // ============ STATE FLOWS ============

    // All touch events (for heatmap)
    private val _touchEvents = MutableStateFlow<List<TouchEvent>>(emptyList())
    val touchEvents: StateFlow<List<TouchEvent>> = _touchEvents.asStateFlow()

    // Centroids from K-Means clustering
    private val _centroids = MutableStateFlow<List<Pair<Float, Float>>>(emptyList())
    val centroids: StateFlow<List<Pair<Float, Float>>> = _centroids.asStateFlow()

    // Mis-tap count (taps that missed the recommended zones)
    private val _misTapCount = MutableStateFlow(0)
    val misTapCount: StateFlow<Int> = _misTapCount.asStateFlow()

    // Mis-tap coordinates
    private val _misTapLocations = MutableStateFlow<List<Pair<Float, Float>>>(emptyList())
    val misTapLocations: StateFlow<List<Pair<Float, Float>>> = _misTapLocations.asStateFlow()

    // Lag Timeline data (for chart)
    private val _lagSpikes = MutableStateFlow<List<LagSpike>>(emptyList())
    val lagSpikes: StateFlow<List<LagSpike>> = _lagSpikes.asStateFlow()

    // Is K-Means processing
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Current session ID for organizing recordings
    private var currentSessionId: Long = 0

    // ============ DATABASE REFERENCE ============

    private val database = AppDatabase.getInstance(FSPEDXApplication.instance)

    // ============ PUBLIC METHODS ============

    /**
     * Load all touch events from the database (reactive).
     */
    fun loadTouchEvents() {
        viewModelScope.launch {
            try {
                database.touchEventDao().getAll().collect { events ->
                    _touchEvents.value = events
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Load touch events for a specific session (by timestamp range).
     */
    fun loadTouchEventsForSession(startTime: Long, endTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val events = database.touchEventDao().getByTimestampRange(startTime, endTime)
                withContext(Dispatchers.Main) {
                    _touchEvents.value = events
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Run K-Means clustering on the loaded touch events.
     * @param k The number of clusters (buttons) to find.
     */
    fun findButtonCentroids(k: Int = 4) {
        if (_touchEvents.value.isEmpty()) {
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true

            withContext(Dispatchers.IO) {
                try {
                    // Convert TouchEvents to KMeansCluster.TouchPoint
                    val points = _touchEvents.value.map { event ->
                        KMeansCluster.TouchPoint(event.x, event.y)
                    }

                    // Run K-Means
                    val centroids = KMeansCluster.findCentroids(points, k)

                    withContext(Dispatchers.Main) {
                        _centroids.value = centroids.map { Pair(it.x, it.y) }
                        _isProcessing.value = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        _isProcessing.value = false
                    }
                }
            }
        }
    }

    /**
     * Detect mis-taps based on the calculated centroids.
     * A mis-tap is a tap that is > 100px from any centroid.
     */
    fun detectMisTaps() {
        if (_centroids.value.isEmpty() || _touchEvents.value.isEmpty()) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val centroidsList = _centroids.value.map { KMeansCluster.TouchPoint(it.first, it.second) }
                val events = _touchEvents.value
                val misTaps = mutableListOf<Pair<Float, Float>>()
                val threshold = 100f // 100px radius

                for (event in events) {
                    val point = KMeansCluster.TouchPoint(event.x, event.y)
                    val distances = centroidsList.map { centroid ->
                        val dx = point.x - centroid.x
                        val dy = point.y - centroid.y
                        kotlin.math.sqrt(dx * dx + dy * dy)
                    }
                    val minDistance = distances.minOrNull() ?: Float.MAX_VALUE
                    if (minDistance > threshold) {
                        misTaps.add(Pair(event.x, event.y))
                    }
                }

                withContext(Dispatchers.Main) {
                    _misTapCount.value = misTaps.size
                    _misTapLocations.value = misTaps
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Load lag spikes from the database (reactive).
     */
    fun loadLagSpikes(sessionId: Long = currentSessionId) {
        viewModelScope.launch {
            try {
                database.lagSpikeDao().getAll().collect { spikes ->
                    _lagSpikes.value = spikes
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Clear all touch events from the database (start fresh).
     */
    fun clearTouchData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.touchEventDao().deleteAll()
                withContext(Dispatchers.Main) {
                    _touchEvents.value = emptyList()
                    _centroids.value = emptyList()
                    _misTapCount.value = 0
                    _misTapLocations.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Clear all lag spikes from the database.
     */
    fun clearLagSpikes() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.lagSpikeDao().deleteAll()
                withContext(Dispatchers.Main) {
                    _lagSpikes.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Export touch data as JSON for backup.
     */
    suspend fun exportTouchData(): String {
        return withContext(Dispatchers.IO) {
            try {
                val events = _touchEvents.value
                val gson = com.google.gson.Gson()
                gson.toJson(events)
            } catch (e: Exception) {
                "{}"
            }
        }
    }

    /**
     * Import touch data from JSON string.
     */
    suspend fun importTouchData(json: String) {
        withContext(Dispatchers.IO) {
            try {
                val gson = com.google.gson.Gson()
                val events = gson.fromJson(json, Array<TouchEvent>::class.java).toList()
                database.touchEventDao().insertAll(events)
                withContext(Dispatchers.Main) {
                    _touchEvents.value = events
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ============ CLEANUP ============

    override fun onCleared() {
        super.onCleared()
        // Close database connection if needed
    }
}
