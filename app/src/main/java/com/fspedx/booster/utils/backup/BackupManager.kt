package com.fspedx.booster.utils.backup

import android.content.Context
import android.os.Environment
import com.fspedx.booster.FSPEDXApplication
import com.fspedx.booster.data.database.AppDatabase
import com.fspedx.booster.utils.constants.Constants
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Backup and restore utilities.
 * Exports settings and touch data to a JSON file in the Downloads folder.
 */
object BackupManager {

    private val gson = Gson()

    /**
     * Export all settings and touch data to a JSON file.
     * @param context Application context.
     * @return true if successful, false otherwise.
     */
    suspend fun exportSettings(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val backupDir = getBackupDir(context)
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "FSPEDX_Backup_$timestamp.json"
                val file = File(backupDir, fileName)

                // Build backup data
                val backupData = JsonObject()

                // 1. Settings (SharedPreferences)
                val prefs = FSPEDXApplication.instance.prefs
                val settings = JsonObject()
                prefs.all.forEach { (key, value) ->
                    settings.addProperty(key, value.toString())
                }
                backupData.add("settings", settings)

                // 2. Touch events (from database)
                val db = AppDatabase.getInstance(context)
                val touchEvents = db.touchEventDao().getAllSnapshot()
                val eventsJson = gson.toJsonTree(touchEvents)
                backupData.add("touchEvents", eventsJson)

                // 3. Lag spikes (from database)
                val lagSpikes = db.lagSpikeDao().getAllSnapshot()
                val spikesJson = gson.toJsonTree(lagSpikes)
                backupData.add("lagSpikes", spikesJson)

                // 4. Whitelist
                val whitelist = prefs.getStringSet(Constants.KEY_WHITELIST_APPS, emptySet()) ?: emptySet()
                val whitelistJson = gson.toJsonTree(whitelist.toList())
                backupData.add("whitelist", whitelistJson)

                // Write to file
                FileWriter(file).use { writer ->
                    gson.toJson(backupData, writer)
                }

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Import settings and touch data from a JSON file.
     * @param context Application context.
     * @param filePath The path to the backup file.
     * @return true if successful, false otherwise.
     */
    suspend fun importSettings(context: Context, filePath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@withContext false
                }

                val json = FileReader(file).use { reader ->
                    gson.fromJson(reader, JsonObject::class.java)
                }

                // 1. Restore settings
                val settings = json.getAsJsonObject("settings")
                val prefs = FSPEDXApplication.instance.prefs
                val editor = prefs.edit()
                settings.entrySet().forEach { entry ->
                    val value = entry.value.asString
                    // Try to parse the value type
                    when {
                        value == "true" || value == "false" -> editor.putBoolean(entry.key, value.toBoolean())
                        value.toIntOrNull() != null -> editor.putInt(entry.key, value.toInt())
                        value.toLongOrNull() != null -> editor.putLong(entry.key, value.toLong())
                        value.toFloatOrNull() != null -> editor.putFloat(entry.key, value.toFloat())
                        else -> editor.putString(entry.key, value)
                    }
                }
                editor.apply()

                // 2. Restore whitelist
                val whitelistArray = json.getAsJsonArray("whitelist")
                val whitelistSet = mutableSetOf<String>()
                whitelistArray.forEach { element ->
                    whitelistSet.add(element.asString)
                }
                prefs.edit().putStringSet(Constants.KEY_WHITELIST_APPS, whitelistSet).apply()

                // 3. Restore touch events
                val db = AppDatabase.getInstance(context)
                val eventsArray = json.getAsJsonArray("touchEvents")
                val touchEvents = eventsArray.map { element ->
                    gson.fromJson(element, com.fspedx.booster.data.models.TouchEvent::class.java)
                }
                // Clear existing and insert new
                db.touchEventDao().deleteAll()
                if (touchEvents.isNotEmpty()) {
                    db.touchEventDao().insertAll(touchEvents)
                }

                // 4. Restore lag spikes
                val spikesArray = json.getAsJsonArray("lagSpikes")
                val lagSpikes = spikesArray.map { element ->
                    gson.fromJson(element, com.fspedx.booster.data.models.LagSpike::class.java)
                }
                db.lagSpikeDao().deleteAll()
                if (lagSpikes.isNotEmpty()) {
                    db.lagSpikeDao().insertAll(lagSpikes)
                }

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Get the backup directory in Downloads.
     */
    private fun getBackupDir(context: Context): File {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            ?.resolve(Constants.BACKUP_DIR)
            ?: File(context.filesDir, Constants.BACKUP_DIR)
    }

    /**
     * List all backup files in the backup directory.
     */
    suspend fun listBackupFiles(context: Context): List<File> {
        return withContext(Dispatchers.IO) {
            val backupDir = getBackupDir(context)
            if (!backupDir.exists()) {
                return@withContext emptyList()
            }
            backupDir.listFiles()?.filter { it.extension == "json" && it.name.startsWith("FSPEDX_Backup_") }
                ?: emptyList()
        }
    }

    /**
     * Delete a backup file.
     */
    suspend fun deleteBackupFile(context: Context, fileName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(getBackupDir(context), fileName)
                file.delete()
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Validate a backup file before import.
     * Checks if the file contains valid JSON and has the required fields.
     */
    suspend fun validateBackupFile(filePath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) return@withContext false

                val json = FileReader(file).use { reader ->
                    gson.fromJson(reader, JsonObject::class.java)
                }

                // Check for required fields
                json.has("settings") && json.has("touchEvents")
            } catch (e: Exception) {
                false
            }
        }
    }
}
