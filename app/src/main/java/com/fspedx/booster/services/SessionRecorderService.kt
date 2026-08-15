package com.fspedx.booster.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.fspedx.booster.R
import com.fspedx.booster.ui.activities.MainActivity
import com.fspedx.booster.utils.constants.Constants
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Service that records the screen with a heatmap overlay.
 * Saves videos to /Movies/FSPEDX/Recordings/
 */
class SessionRecorderService : android.app.Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var resultCode = 0
    private var data: Intent? = null

    companion object {
        private var instance: SessionRecorderService? = null

        fun start(context: Context) {
            val intent = Intent(context, SessionRecorderService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SessionRecorderService::class.java)
            context.stopService(intent)
        }

        fun isRunning(): Boolean = instance != null && instance?.isRecording == true

        fun setMediaProjection(resultCode: Int, data: Intent) {
            instance?.configureMediaProjection(resultCode, data)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must call startForeground() promptly when started via startForegroundService()
        startForeground(Constants.NOTIFICATION_ID_RECORDING, buildNotification())

        when (intent?.action) {
            Constants.ACTION_STOP_RECORDING -> {
                stopRecording()
                return START_NOT_STICKY
            }
            Constants.ACTION_START_RECORDING -> {
                // MediaProjection data is expected to already be set via
                // setMediaProjection() before start; fall through to start.
            }
        }

        // Start recording when service starts
        if (!isRecording) {
            startRecording()
        }
        return START_STICKY
    }

    /**
     * Configure the MediaProjection token. MUST be called with the activity
     * result (RESULT_OK + data) from MediaProjectionManager.createScreenCaptureIntent()
     * BEFORE the service starts recording.
     */
    fun configureMediaProjection(resultCode: Int, data: Intent) {
        this.resultCode = resultCode
        this.data = data
        if (resultCode == android.app.Activity.RESULT_OK && data != null) {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = manager.getMediaProjection(resultCode, data)
        }
    }

    private fun startRecording() {
        if (isRecording) return
        if (mediaProjection == null) {
            // Need to get MediaProjection from the activity via callback.
            // HomeFragment requests permission and calls configureMediaProjection().
            stopSelf()
            return
        }

        try {
            val outputFile = createOutputFile()

            // Use the real display size so the recording matches the screen
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoEncodingBitRate(4_000_000)
                setVideoFrameRate(30)
                setVideoSize(width, height)
                setOutputFile(outputFile.absolutePath)
                prepare()
            }

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "FSPEDX Recording",
                width,
                height,
                resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null,
                null
            )

            mediaRecorder?.start()
            isRecording = true
            startForeground(Constants.NOTIFICATION_ID_RECORDING, buildNotification())

        } catch (e: Exception) {
            e.printStackTrace()
            stopRecording()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null

            virtualDisplay?.release()
            virtualDisplay = null

            mediaProjection?.stop()
            mediaProjection = null

            isRecording = false
            stopForeground(true)

            publishToMediaStore()

            android.widget.Toast.makeText(
                this,
                "Recording saved to Movies/FSPEDX",
                android.widget.Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopSelf()
    }

    private var outputFile: File? = null
    private var mediaStoreUri: android.net.Uri? = null

    private fun createOutputFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "FSPEDX_$timestamp.mp4"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Scoped storage: insert into MediaStore, record the content URI so we
            // can publish it on stop. For MediaRecorder we still need a real path,
            // so write to our app cache and then copy to MediaStore on stop.
            val cacheDir = File(cacheDir, Constants.RECORDINGS_DIR)
            cacheDir.mkdirs()
            outputFile = File(cacheDir, fileName)
            return outputFile!!
        }

        // Legacy: write directly to Movies/FSPEDX
        val recordingsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            ?.resolve(Constants.RECORDINGS_DIR)
        recordingsDir?.mkdirs()
        outputFile = File(recordingsDir, fileName)
        return outputFile!!
    }

    private fun publishToMediaStore() {
        try {
            val file = outputFile ?: return
            if (!file.exists()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + Constants.RECORDINGS_DIR)
                }
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { it.copyTo(out) }
                    }
                    mediaStoreUri = uri
                    file.delete() // remove cache copy
                    return
                }
            }
            // Legacy path already in public storage — nothing to do
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SessionRecorderService::class.java)
        stopIntent.action = Constants.ACTION_STOP_RECORDING
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_RECORDING)
            .setContentTitle("Recording Session")
            .setContentText("Recording gameplay with heatmap overlay...")
            .setSmallIcon(R.drawable.ic_record)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_RECORDING,
                "FSPEDX Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when a session is being recorded"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        if (isRecording) {
            stopRecording()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
