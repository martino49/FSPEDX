package com.fspedx.booster.data.repository

import android.content.Context
import com.fspedx.booster.data.database.AppDatabase
import com.fspedx.booster.data.models.LagSpike
import com.fspedx.booster.data.models.TouchEvent
import com.fspedx.booster.utils.constants.Constants
import com.fspedx.booster.utils.network.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Repository that bridges ViewModels with the data sources.
 * Handles both local (Room) and remote (Network) data.
 */
class StatsRepository(
    private val context: Context = com.fspedx.booster.FSPEDXApplication.instance
) {

    // Lazy init the database
    private val database by lazy { AppDatabase.getInstance(context) }
    private val touchEventDao by lazy { database.touchEventDao() }
    private val lagSpikeDao by lazy { database.lagSpikeDao() }

    // ============ TOUCH EVENTS ============

    /**
     * Get all touch events as a Flow (reactive).
     */
    fun getAllTouchEvents(): Flow<List<TouchEvent>> {
        return touchEventDao.getAll()
    }

    /**
     * Get touch events by timestamp range (suspending).
     */
    suspend fun getTouchEventsByRange(startTime: Long, endTime: Long): List<TouchEvent> {
        return withContext(Dispatchers.IO) {
            touchEventDao.getByTimestampRange(startTime, endTime)
        }
    }

    /**
     * Insert a single touch event.
     */
    suspend fun insertTouchEvent(event: TouchEvent) {
        withContext(Dispatchers.IO) {
            touchEventDao.insert(event)
        }
    }

    /**
     * Insert multiple touch events.
     */
    suspend fun insertTouchEvents(events: List<TouchEvent>) {
        withContext(Dispatchers.IO) {
            touchEventDao.insertAll(events)
        }
    }

    /**
     * Delete all touch events.
     */
    suspend fun clearTouchEvents() {
        withContext(Dispatchers.IO) {
            touchEventDao.deleteAll()
        }
    }

    /**
     * Count total touch events.
     */
    suspend fun getTouchEventCount(): Int {
        return withContext(Dispatchers.IO) {
            touchEventDao.getCount()
        }
    }

    // ============ LAG SPIKES ============

    /**
     * Get all lag spikes as a Flow (reactive).
     */
    fun getAllLagSpikes(): Flow<List<LagSpike>> {
        return lagSpikeDao.getAll()
    }

    /**
     * Get lag spikes by session ID.
     */
    suspend fun getLagSpikesBySession(sessionId: Long): List<LagSpike> {
        return withContext(Dispatchers.IO) {
            lagSpikeDao.getBySession(sessionId)
        }
    }

    /**
     * Insert a single lag spike.
     */
    suspend fun insertLagSpike(spike: LagSpike) {
        withContext(Dispatchers.IO) {
            lagSpikeDao.insert(spike)
        }
    }

    /**
     * Insert multiple lag spikes.
     */
    suspend fun insertLagSpikes(spikes: List<LagSpike>) {
        withContext(Dispatchers.IO) {
            lagSpikeDao.insertAll(spikes)
        }
    }

    /**
     * Delete all lag spikes.
     */
    suspend fun clearLagSpikes() {
        withContext(Dispatchers.IO) {
            lagSpikeDao.deleteAll()
        }
    }

    // ============ NETWORK OPERATIONS ============

    /**
     * Perform a ping test to the default host.
     * @return Ping in milliseconds, or -1 if failed.
     */
    suspend fun runPingTest(host: String = "8.8.8.8"): Int {
        return withContext(Dispatchers.IO) {
            NetworkUtils.ping(host)
        }
    }

    /**
     * Perform a ping test and automatically save it as a LagSpike.
     * @return The ping value, or -1 if failed.
     */
    suspend fun runAndSavePingTest(
        sessionId: Long = 0,
        host: String = "8.8.8.8"
    ): Int {
        return withContext(Dispatchers.IO) {
            val ping = NetworkUtils.ping(host)
            if (ping > 0) {
                val spike = LagSpike(
                    timestamp = System.currentTimeMillis(),
                    ping = ping,
                    sessionId = sessionId
                )
                lagSpikeDao.insert(spike)
            }
            ping
        }
    }

    /**
     * Start a continuous ping monitoring session.
     * Pings every 2 seconds for the given duration and records LagSpikes.
     * @param sessionId Session identifier for grouping.
     * @param host Host to ping.
     * @param durationMs How long to monitor for (0 = until cancelled by caller).
     */
    suspend fun startPingMonitoring(
        sessionId: Long,
        host: String = Constants.PING_DEFAULT_HOST,
        durationMs: Long = 0L
    ) {
        val startTime = System.currentTimeMillis()
        // Use coroutineScope so cancellation from the caller stops this loop.
        kotlinx.coroutines.coroutineScope {
            while (isActive) {
                val ping = NetworkUtils.ping(host, Constants.PING_TIMEOUT_MS)
                if (ping > 0) {
                    lagSpikeDao.insert(
                        LagSpike(
                            timestamp = System.currentTimeMillis(),
                            ping = ping,
                            sessionId = sessionId
                        )
                    )
                }
                // Respect the optional duration limit
                if (durationMs > 0 && System.currentTimeMillis() - startTime >= durationMs) {
                    break
                }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    // ============ DATA EXPORT/IMPORT ============

    /**
     * Export all touch events as a JSON string.
     */
    suspend fun exportTouchData(): String {
        return withContext(Dispatchers.IO) {
            try {
                val events = touchEventDao.getAllSnapshot()
                val gson = com.google.gson.Gson()
                gson.toJson(events)
            } catch (e: Exception) {
                "{}"
            }
        }
    }
}
