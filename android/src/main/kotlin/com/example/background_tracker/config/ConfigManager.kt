package com.example.background_tracker.config

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

class ConfigManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("TrackerConfig", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CONFIG_JSON = "config_json"
    }

    fun saveConfig(configMap: Map<String, Any>) {
        val json = JSONObject(configMap).toString()
        prefs.edit().putString(KEY_CONFIG_JSON, json).apply()
    }

    fun getConfig(): Config? {
        val jsonString = prefs.getString(KEY_CONFIG_JSON, null) ?: return null
        return try {
            val json = JSONObject(jsonString)
            Config(
                syncUrl = json.optString("syncUrl").takeIf { it.isNotEmpty() && it != "null" },
                syncHeaders = parseHeaders(json.optJSONObject("syncHeaders")),
                syncBatchSize = json.optInt("syncBatchSize", 100),
                syncInterval = json.optInt("syncInterval", 300),
                distanceFilter = json.optDouble("distanceFilter", 10.0).toFloat(),
                timeInterval = json.optLong("timeInterval", 10000),
                accuracyMode = json.optString("accuracyMode", "balanced")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseHeaders(json: JSONObject?): Map<String, String> {
        val map = mutableMapOf<String, String>()
        json?.let {
            val keys = it.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = it.getString(key)
            }
        }
        return map
    }

    data class Config(
        val syncUrl: String?,
        val syncHeaders: Map<String, String>,
        val syncBatchSize: Int,
        val syncInterval: Int,
        val distanceFilter: Float,
        val timeInterval: Long,
        val accuracyMode: String
    )
}
