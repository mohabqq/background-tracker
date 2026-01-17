package com.example.background_tracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.background_tracker.database.LocationDatabase
import com.example.background_tracker.motion.MotionDetector
import com.example.background_tracker.activity.ActivityRecognizer
import com.example.background_tracker.sync.SyncManager
import com.example.background_tracker.battery.BatteryOptimizer

class LocationService : Service(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var locationDatabase: LocationDatabase
    private lateinit var motionDetector: MotionDetector
    private lateinit var activityRecognizer: ActivityRecognizer
    private lateinit var syncManager: SyncManager
    private lateinit var batteryOptimizer: BatteryOptimizer
    private val CHANNEL_ID = "background_tracker_channel"
    
    private var isTrackingPaused = false
    private var isLowPowerMode = false
    private var currentActivity = "UNKNOWN"
    private var currentDistanceFilter = 10f
    private var currentTimeInterval = 10000L

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationDatabase = LocationDatabase(this)
        
        // Initialize motion detector
        motionDetector = MotionDetector(this) { isMoving ->
            handleMotionChanged(isMoving)
        }
        
        // Initialize activity recognizer
        activityRecognizer = ActivityRecognizer(this) { activity, confidence ->
            handleActivityChanged(activity, confidence)
        }
        
        // Initialize sync manager
        syncManager = SyncManager(this)
        
        // Initialize battery optimizer
        batteryOptimizer = BatteryOptimizer(this) { isLowPower, isCharging ->
            handleBatteryStateChanged(isLowPower, isCharging)
        }

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)

        // Start motion detection and activity recognition
        motionDetector.start()
        activityRecognizer.start()
        
        // Start sync manager
        syncManager.start()
        
        // Start battery optimization
        batteryOptimizer.start()
        
        startLocationUpdates()

        return START_STICKY
    }

    private fun startLocationUpdates() {
        if (isTrackingPaused) {
            android.util.Log.d("LocationService", "Tracking is paused (stationary)")
            return
        }
        
        try {
            // Remove existing updates first
            locationManager.removeUpdates(this)
            
            // Network: Faster but less accurate
            // Only use network provider if we are in low power mode to save battery
            // Or if we want faster updates alongside GPS
            if (isLowPowerMode) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    currentTimeInterval * 2, // Less frequent in low power
                    currentDistanceFilter * 2,
                    this,
                    Looper.getMainLooper()
                )
                android.util.Log.i("LocationService", "Low Power Mode: Using Network Provider only")
            } else {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    currentTimeInterval,
                    currentDistanceFilter,
                    this,
                    Looper.getMainLooper()
                )
                // Optionally add Network provider for faster initial fix, but GPS is primary
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    currentTimeInterval,
                    currentDistanceFilter,
                    this,
                    Looper.getMainLooper()
                )
                android.util.Log.d("LocationService", "High Accuracy Mode: Using GPS & Network")
            }
            
            android.util.Log.d("LocationService", "Location updates started (distance: ${currentDistanceFilter}m, interval: ${currentTimeInterval}ms, activity: $currentActivity)")
        } catch (e: SecurityException) {
            android.util.Log.e("LocationService", "Permission denied", e)
            e.printStackTrace()
        }
    }

    private fun removeLocationUpdates() {
        locationManager.removeUpdates(this)
    }

    override fun onLocationChanged(location: Location) {
        android.util.Log.d("LocationService", "Location received: ${location.latitude}, ${location.longitude}")
        
        // Store in database
        try {
            locationDatabase.insertLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy.toDouble(),
                altitude = if (location.hasAltitude()) location.altitude else null,
                speed = if (location.hasSpeed()) location.speed.toDouble() else null,
                bearing = if (location.hasBearing()) location.bearing.toDouble() else null,
                timestamp = location.time
            )
        } catch (e: Exception) {
            android.util.Log.e("LocationService", "Failed to store location in database", e)
        }
        
        // Send location to Flutter via Broadcast
        val intent = Intent("com.example.background_tracker.LOCATION_UPDATE")
        intent.putExtra("latitude", location.latitude)
        intent.putExtra("longitude", location.longitude)
        sendBroadcast(intent)
        
        android.util.Log.d("LocationService", "Broadcast sent")
    }

    private fun handleMotionChanged(isMoving: Boolean) {
        android.util.Log.i("LocationService", "Motion changed: ${if (isMoving) "MOVING" else "STATIONARY"}")
        
        if (!isMoving) {
            // Device is stationary - pause tracking to save battery
            isTrackingPaused = true
            removeLocationUpdates()
            android.util.Log.i("LocationService", "Tracking PAUSED (device stationary)")
        } else {
            // Device started moving - resume tracking
            isTrackingPaused = false
            startLocationUpdates()
            android.util.Log.i("LocationService", "Tracking RESUMED (device moving)")
        }
    }

    private fun handleActivityChanged(activity: String, confidence: Int) {
        android.util.Log.i("LocationService", "Activity changed: $activity (confidence: $confidence%)")
        currentActivity = activity
        
        // Get recommended settings for this activity
        val (distanceFilter, timeInterval) = activityRecognizer.getRecommendedSettings()
        
        if (distanceFilter == 0f && timeInterval == 0L) {
            // Activity is STILL - pause tracking
            isTrackingPaused = true
            removeLocationUpdates()
            android.util.Log.i("LocationService", "Tracking PAUSED (activity: STILL)")
        } else {
            // Update tracking parameters
            currentDistanceFilter = distanceFilter
            currentTimeInterval = timeInterval
            
            if (isTrackingPaused) {
                isTrackingPaused = false
            }
            
            // Restart location updates with new parameters
            startLocationUpdates()
            android.util.Log.i("LocationService", "Tracking parameters updated for activity: $activity")
        }
    }

    private fun handleBatteryStateChanged(isLowPower: Boolean, isCharging: Boolean) {
        android.util.Log.i("LocationService", "Battery state changed. LowPower: $isLowPower, Charging: $isCharging")
        if (isLowPower != isLowPowerMode) {
            isLowPowerMode = isLowPower
            startLocationUpdates() // Restart to apply new providers/settings
        }
    }

    // Boilerplate for Notification Channel (Android O+)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Background Tracker Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tracking Location")
            .setContentText("Your location is being tracked in the background")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeLocationUpdates()
        motionDetector.stop()
        activityRecognizer.stop()
        syncManager.stop()
        batteryOptimizer.stop()
        android.util.Log.d("LocationService", "Service destroyed, all tracking stopped")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
