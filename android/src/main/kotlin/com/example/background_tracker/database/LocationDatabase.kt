package com.example.background_tracker.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class LocationDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "LocationDatabase"
        private const val DATABASE_NAME = "background_tracker.db"
        private const val DATABASE_VERSION = 1

        // Table name
        private const val TABLE_LOCATIONS = "locations"

        // Column names
        private const val COLUMN_ID = "id"
        private const val COLUMN_LATITUDE = "latitude"
        private const val COLUMN_LONGITUDE = "longitude"
        private const val COLUMN_ACCURACY = "accuracy"
        private const val COLUMN_ALTITUDE = "altitude"
        private const val COLUMN_SPEED = "speed"
        private const val COLUMN_BEARING = "bearing"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_ACTIVITY = "activity"
        private const val COLUMN_BATTERY_LEVEL = "battery_level"
        private const val COLUMN_IS_SYNCED = "is_synced"
        private const val COLUMN_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_LOCATIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_LATITUDE REAL NOT NULL,
                $COLUMN_LONGITUDE REAL NOT NULL,
                $COLUMN_ACCURACY REAL,
                $COLUMN_ALTITUDE REAL,
                $COLUMN_SPEED REAL,
                $COLUMN_BEARING REAL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_ACTIVITY TEXT,
                $COLUMN_BATTERY_LEVEL INTEGER,
                $COLUMN_IS_SYNCED INTEGER DEFAULT 0,
                $COLUMN_CREATED_AT INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """.trimIndent()

        db.execSQL(createTableQuery)

        // Create indexes for better query performance
        db.execSQL("CREATE INDEX idx_timestamp ON $TABLE_LOCATIONS($COLUMN_TIMESTAMP)")
        db.execSQL("CREATE INDEX idx_synced ON $TABLE_LOCATIONS($COLUMN_IS_SYNCED)")

        Log.d(TAG, "Database created successfully")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOCATIONS")
        onCreate(db)
    }

    /**
     * Insert a new location into the database
     */
    fun insertLocation(
        latitude: Double,
        longitude: Double,
        accuracy: Double? = null,
        altitude: Double? = null,
        speed: Double? = null,
        bearing: Double? = null,
        timestamp: Long,
        activity: String? = null,
        batteryLevel: Int? = null
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LATITUDE, latitude)
            put(COLUMN_LONGITUDE, longitude)
            put(COLUMN_ACCURACY, accuracy)
            put(COLUMN_ALTITUDE, altitude)
            put(COLUMN_SPEED, speed)
            put(COLUMN_BEARING, bearing)
            put(COLUMN_TIMESTAMP, timestamp)
            put(COLUMN_ACTIVITY, activity)
            put(COLUMN_BATTERY_LEVEL, batteryLevel)
            put(COLUMN_IS_SYNCED, 0)
        }

        val id = db.insert(TABLE_LOCATIONS, null, values)
        Log.d(TAG, "Inserted location: $latitude, $longitude (ID: $id)")
        return id
    }

    /**
     * Batch insert multiple locations
     */
    fun insertLocations(locations: List<Map<String, Any?>>): Int {
        val db = writableDatabase
        var count = 0

        db.beginTransaction()
        try {
            for (location in locations) {
                val values = ContentValues().apply {
                    put(COLUMN_LATITUDE, location["latitude"] as Double)
                    put(COLUMN_LONGITUDE, location["longitude"] as Double)
                    put(COLUMN_ACCURACY, location["accuracy"] as Double?)
                    put(COLUMN_ALTITUDE, location["altitude"] as Double?)
                    put(COLUMN_SPEED, location["speed"] as Double?)
                    put(COLUMN_BEARING, location["bearing"] as Double?)
                    put(COLUMN_TIMESTAMP, location["timestamp"] as Long)
                    put(COLUMN_ACTIVITY, location["activity"] as String?)
                    put(COLUMN_BATTERY_LEVEL, location["batteryLevel"] as Int?)
                    put(COLUMN_IS_SYNCED, 0)
                }
                if (db.insert(TABLE_LOCATIONS, null, values) != -1L) {
                    count++
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        Log.d(TAG, "Batch inserted $count locations")
        return count
    }

    /**
     * Get unsynced locations (for HTTP sync)
     */
    fun getUnsyncedLocations(limit: Int = 100): List<Map<String, Any?>> {
        val db = readableDatabase
        val locations = mutableListOf<Map<String, Any?>>()

        val cursor = db.query(
            TABLE_LOCATIONS,
            null,
            "$COLUMN_IS_SYNCED = ?",
            arrayOf("0"),
            null,
            null,
            "$COLUMN_TIMESTAMP ASC",
            limit.toString()
        )

        cursor.use {
            while (it.moveToNext()) {
                locations.add(mapOf(
                    "id" to it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                    "latitude" to it.getDouble(it.getColumnIndexOrThrow(COLUMN_LATITUDE)),
                    "longitude" to it.getDouble(it.getColumnIndexOrThrow(COLUMN_LONGITUDE)),
                    "accuracy" to it.getDoubleOrNull(it.getColumnIndexOrThrow(COLUMN_ACCURACY)),
                    "altitude" to it.getDoubleOrNull(it.getColumnIndexOrThrow(COLUMN_ALTITUDE)),
                    "speed" to it.getDoubleOrNull(it.getColumnIndexOrThrow(COLUMN_SPEED)),
                    "bearing" to it.getDoubleOrNull(it.getColumnIndexOrThrow(COLUMN_BEARING)),
                    "timestamp" to it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                    "activity" to it.getStringOrNull(it.getColumnIndexOrThrow(COLUMN_ACTIVITY)),
                    "batteryLevel" to it.getIntOrNull(it.getColumnIndexOrThrow(COLUMN_BATTERY_LEVEL))
                ))
            }
        }

        Log.d(TAG, "Retrieved ${locations.size} unsynced locations")
        return locations
    }

    /**
     * Mark locations as synced
     */
    fun markAsSynced(ids: List<Long>): Int {
        if (ids.isEmpty()) return 0

        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_SYNCED, 1)
        }

        val count = db.update(
            TABLE_LOCATIONS,
            values,
            "$COLUMN_ID IN (${ids.joinToString(",")})",
            null
        )

        Log.d(TAG, "Marked $count locations as synced")
        return count
    }

    /**
     * Delete old locations based on retention policy
     */
    fun deleteOldLocations(retentionDays: Int): Int {
        val db = writableDatabase
        val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)

        val count = db.delete(
            TABLE_LOCATIONS,
            "$COLUMN_TIMESTAMP < ? AND $COLUMN_IS_SYNCED = ?",
            arrayOf(cutoffTime.toString(), "1")
        )

        Log.d(TAG, "Deleted $count old locations (older than $retentionDays days)")
        return count
    }

    /**
     * Get total count of locations
     */
    fun getLocationCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_LOCATIONS", null)
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }
        return 0
    }

    /**
     * Clear all locations
     */
    fun clearAll(): Int {
        val db = writableDatabase
        val count = db.delete(TABLE_LOCATIONS, null, null)
        Log.d(TAG, "Cleared all $count locations")
        return count
    }

    // Helper extensions
    private fun android.database.Cursor.getDoubleOrNull(columnIndex: Int): Double? {
        return if (isNull(columnIndex)) null else getDouble(columnIndex)
    }

    private fun android.database.Cursor.getStringOrNull(columnIndex: Int): String? {
        return if (isNull(columnIndex)) null else getString(columnIndex)
    }

    private fun android.database.Cursor.getIntOrNull(columnIndex: Int): Int? {
        return if (isNull(columnIndex)) null else getInt(columnIndex)
    }
}
