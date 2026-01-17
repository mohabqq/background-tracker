package com.example.background_tracker.activity

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

/**
 * Recognizes user activity (walking, running, driving, cycling, still)
 * Uses Google Play Services Activity Recognition API
 */
class ActivityRecognizer(
    private val context: Context,
    private val onActivityChanged: (String, Int) -> Unit
) {

    companion object {
        private const val TAG = "ActivityRecognizer"
        private const val ACTION_ACTIVITY_UPDATE = "com.example.background_tracker.ACTIVITY_UPDATE"
        
        // Activity type names
        const val ACTIVITY_STILL = "STILL"
        const val ACTIVITY_WALKING = "WALKING"
        const val ACTIVITY_RUNNING = "RUNNING"
        const val ACTIVITY_DRIVING = "DRIVING"
        const val ACTIVITY_CYCLING = "CYCLING"
        const val ACTIVITY_UNKNOWN = "UNKNOWN"
    }

    private var currentActivity = ACTIVITY_UNKNOWN
    private var currentConfidence = 0

    private val activityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (ActivityRecognitionResult.hasResult(intent)) {
                val result = ActivityRecognitionResult.extractResult(intent!!)
                handleActivityResult(result)
            }
        }
    }

    /**
     * Start activity recognition
     */
    fun start() {
        try {
            // Register broadcast receiver
            val filter = IntentFilter(ACTION_ACTIVITY_UPDATE)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(activityReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(activityReceiver, filter)
            }

            // Create pending intent for activity updates
            val intent = Intent(ACTION_ACTIVITY_UPDATE)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            // Request activity updates every 10 seconds
            ActivityRecognition.getClient(context)
                .requestActivityUpdates(10000, pendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Activity recognition started successfully")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to start activity recognition", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting activity recognition", e)
        }
    }

    /**
     * Stop activity recognition
     */
    fun stop() {
        try {
            context.unregisterReceiver(activityReceiver)
            
            val intent = Intent(ACTION_ACTIVITY_UPDATE)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            ActivityRecognition.getClient(context)
                .removeActivityUpdates(pendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Activity recognition stopped successfully")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping activity recognition", e)
        }
    }

    private fun handleActivityResult(result: ActivityRecognitionResult?) {
        result?.let {
            val mostProbableActivity = it.mostProbableActivity
            val activityType = getActivityString(mostProbableActivity.type)
            val confidence = mostProbableActivity.confidence

            // Only update if confidence is high enough (>= 50%)
            if (confidence >= 50 && activityType != currentActivity) {
                currentActivity = activityType
                currentConfidence = confidence
                
                Log.i(TAG, "Activity changed: $activityType (confidence: $confidence%)")
                onActivityChanged(activityType, confidence)
            }
        }
    }

    private fun getActivityString(activityType: Int): String {
        return when (activityType) {
            DetectedActivity.STILL -> ACTIVITY_STILL
            DetectedActivity.WALKING -> ACTIVITY_WALKING
            DetectedActivity.RUNNING -> ACTIVITY_RUNNING
            DetectedActivity.ON_BICYCLE -> ACTIVITY_CYCLING
            DetectedActivity.IN_VEHICLE -> ACTIVITY_DRIVING
            else -> ACTIVITY_UNKNOWN
        }
    }

    /**
     * Get current detected activity
     */
    fun getCurrentActivity(): String = currentActivity

    /**
     * Get confidence of current activity (0-100)
     */
    fun getCurrentConfidence(): Int = currentConfidence

    /**
     * Get recommended GPS settings based on current activity
     * Returns Pair<distanceFilter, timeInterval>
     */
    fun getRecommendedSettings(): Pair<Float, Long> {
        return when (currentActivity) {
            ACTIVITY_STILL -> Pair(0f, 0L) // Pause tracking
            ACTIVITY_WALKING -> Pair(20f, 30000L) // 20m, 30s
            ACTIVITY_RUNNING -> Pair(10f, 15000L) // 10m, 15s
            ACTIVITY_DRIVING -> Pair(50f, 10000L) // 50m, 10s
            ACTIVITY_CYCLING -> Pair(15f, 20000L) // 15m, 20s
            else -> Pair(10f, 10000L) // Default: 10m, 10s
        }
    }
}
