package com.example.background_tracker.sync

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.background_tracker.config.ConfigManager
import com.example.background_tracker.database.LocationDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class SyncManager(private val context: Context) {

    companion object {
        private const val TAG = "SyncManager"
    }

    private val db = LocationDatabase(context)
    private val configManager = ConfigManager(context)
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    
    private var isSyncing = false
    private var syncRunnable: Runnable? = null

    fun start() {
        val config = configManager.getConfig()
        if (config?.syncUrl == null) {
            Log.d(TAG, "No sync URL configured. Sync disabled.")
            return
        }

        Log.d(TAG, "Starting sync manager. Interval: ${config.syncInterval}s")
        scheduleSync(config.syncInterval * 1000L)
    }

    fun stop() {
        syncRunnable?.let { handler.removeCallbacks(it) }
        Log.d(TAG, "Sync manager stopped")
    }

    private fun scheduleSync(intervalMs: Long) {
        syncRunnable = Runnable {
            performSync()
            scheduleSync(intervalMs)
        }
        handler.postDelayed(syncRunnable!!, intervalMs)
    }

    fun forceSync() {
        executor.execute {
            performSync()
        }
    }

    private fun performSync() {
        if (isSyncing) return
        isSyncing = true

        executor.execute {
            try {
                val config = configManager.getConfig()
                if (config?.syncUrl == null) return@execute

                val locations = db.getUnsyncedLocations(config.syncBatchSize)
                if (locations.isEmpty()) {
                    Log.d(TAG, "No unsynced locations found.")
                    return@execute
                }

                Log.d(TAG, "Syncing ${locations.size} locations to ${config.syncUrl}...")

                val jsonPayload = JSONArray()
                val ids = mutableListOf<Long>()

                for (loc in locations) {
                    val jsonObj = JSONObject()
                    jsonObj.put("latitude", loc["latitude"])
                    jsonObj.put("longitude", loc["longitude"])
                    jsonObj.put("timestamp", loc["timestamp"])
                    jsonObj.put("accuracy", loc["accuracy"])
                    jsonObj.put("speed", loc["speed"])
                    jsonObj.put("bearing", loc["bearing"])
                    jsonObj.put("altitude", loc["altitude"])
                    jsonObj.put("activity", loc["activity"])
                    jsonObj.put("batteryLevel", loc["batteryLevel"])
                    
                    jsonPayload.put(jsonObj)
                    ids.add(loc["id"] as Long)
                }

                val success = sendData(config.syncUrl, jsonPayload.toString(), config.syncHeaders)
                
                if (success) {
                    db.markAsSynced(ids)
                    Log.d(TAG, "Successfully synced ${ids.size} locations.")
                    
                    // If we touched the limit, there might be more... sync immediately again?
                    // For now, let's stick to interval to save battery.
                } else {
                    Log.e(TAG, "Failed to sync locations.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during sync", e)
            } finally {
                isSyncing = false
            }
        }
    }

    private fun sendData(urlString: String, jsonBody: String, headers: Map<String, String>): Boolean {
        var urlConnection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "POST"
            urlConnection.doOutput = true
            urlConnection.doInput = true
            urlConnection.connectTimeout = 15000 // 15s
            urlConnection.readTimeout = 15000    // 15s
            
            // Default headers
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            urlConnection.setRequestProperty("Accept", "application/json")
            
            // Custom headers
            headers.forEach { (key, value) ->
                urlConnection.setRequestProperty(key, value)
            }

            val outputStream = OutputStreamWriter(urlConnection.outputStream)
            outputStream.write(jsonBody)
            outputStream.flush()
            outputStream.close()

            val responseCode = urlConnection.responseCode
            Log.d(TAG, "Server returned: $responseCode")
            
            // Consider 2xx keys as success
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Network error", e)
            false
        } finally {
            urlConnection?.disconnect()
        }
    }
}
