package com.example.background_tracker.geofence

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
        private const val CHANNEL_ID = "geofence_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent!!.hasError()) {
            Log.e(TAG, "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        if (triggeringGeofences != null) {
            for (geofence in triggeringGeofences) {
                val geofenceId = geofence.requestId
                handleTransition(context, geofenceId, geofenceTransition)
            }
        }
    }

    private fun handleTransition(context: Context, geofenceId: String, transitionType: Int) {
        val prefs = context.getSharedPreferences("GeofencePrefs", Context.MODE_PRIVATE)
        var message: String? = null
        var transitionName = ""

        when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                message = prefs.getString("${geofenceId}_enter", null)
                transitionName = "ENTER"
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                message = prefs.getString("${geofenceId}_exit", null)
                transitionName = "EXIT"
            }
            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                message = prefs.getString("${geofenceId}_dwell", null)
                transitionName = "DWELL"
            }
            else -> return
        }
        
        Log.i(TAG, "Geofence $transitionName: $geofenceId")
        
        // Broadcast to Flutter
        val flutterIntent = Intent("com.example.background_tracker.GEOFENCE_EVENT")
        flutterIntent.putExtra("id", geofenceId)
        flutterIntent.putExtra("transition", transitionName)
        context.sendBroadcast(flutterIntent)

        // Show notification if message is set
        if (!message.isNullOrBlank()) {
            sendNotification(context, message)
        }
    }

    private fun sendNotification(context: Context, message: String) {
        createNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("Geofence Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Geofence Alerts"
            val descriptionText = "Notifications for geofence entry/exit"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
