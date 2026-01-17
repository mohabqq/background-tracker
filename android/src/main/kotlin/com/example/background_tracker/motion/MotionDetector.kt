package com.example.background_tracker.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

/**
 * Detects device motion using accelerometer to determine if user is stationary or moving
 * This helps save battery by pausing GPS tracking when device is not moving
 */
class MotionDetector(
    context: Context,
    private val onMotionChanged: (Boolean) -> Unit
) : SensorEventListener {

    companion object {
        private const val TAG = "MotionDetector"
        
        // Threshold for detecting significant motion (m/s²)
        private const val MOTION_THRESHOLD = 0.5f
        
        // Time window for averaging acceleration (milliseconds)
        private const val AVERAGING_WINDOW = 2000L
        
        // Number of consecutive stationary readings before declaring device stationary
        private const val STATIONARY_COUNT_THRESHOLD = 5
    }

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private var isMoving = true
    private var lastUpdate = 0L
    private var stationaryCount = 0
    
    // Store recent acceleration values for averaging
    private val accelerationHistory = mutableListOf<Float>()

    /**
     * Start monitoring motion
     */
    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Log.d(TAG, "Motion detection started")
        } ?: run {
            Log.w(TAG, "Accelerometer not available")
        }
    }

    /**
     * Stop monitoring motion
     */
    fun stop() {
        sensorManager.unregisterListener(this)
        Log.d(TAG, "Motion detection stopped")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val currentTime = System.currentTimeMillis()
        
        // Only process updates every AVERAGING_WINDOW milliseconds
        if (currentTime - lastUpdate < AVERAGING_WINDOW) return
        
        lastUpdate = currentTime

        // Calculate total acceleration (magnitude of acceleration vector)
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        // Remove gravity component (9.81 m/s²)
        val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
        
        // Add to history
        accelerationHistory.add(kotlin.math.abs(acceleration))
        
        // Keep only recent values (last 5 readings)
        if (accelerationHistory.size > 5) {
            accelerationHistory.removeAt(0)
        }
        
        // Calculate average acceleration
        val avgAcceleration = accelerationHistory.average().toFloat()
        
        Log.d(TAG, "Avg acceleration: $avgAcceleration m/s²")
        
        // Determine if device is moving or stationary
        if (avgAcceleration < MOTION_THRESHOLD) {
            stationaryCount++
            
            if (stationaryCount >= STATIONARY_COUNT_THRESHOLD && isMoving) {
                // Device is now stationary
                isMoving = false
                Log.i(TAG, "Device is STATIONARY")
                onMotionChanged(false)
            }
        } else {
            stationaryCount = 0
            
            if (!isMoving) {
                // Device started moving
                isMoving = true
                Log.i(TAG, "Device is MOVING")
                onMotionChanged(true)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    /**
     * Get current motion state
     */
    fun isCurrentlyMoving(): Boolean = isMoving
}
