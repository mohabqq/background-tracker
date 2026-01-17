package com.example.background_tracker.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.background_tracker.geofence.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {

    companion object {
        private const val TAG = "GeofenceManager"
    }

    private val geofencingClient = LocationServices.getGeofencingClient(context)
    
    // Store active geofences to handle removals if needed
    private val activeGeofences = mutableListOf<String>()

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun addGeofence(
        id: String,
        latitude: Double,
        longitude: Double,
        radius: Float,
        expirationDuration: Long = Geofence.NEVER_EXPIRE,
        loiteringDelay: Int = 300000,
        notifyOnEntry: String? = null,
        notifyOnExit: String? = null,
        notifyOnDwell: String? = null
    ) {
        val geofence = Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(latitude, longitude, radius)
            .setExpirationDuration(expirationDuration)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or
                Geofence.GEOFENCE_TRANSITION_EXIT or
                Geofence.GEOFENCE_TRANSITION_DWELL
            )
            .setLoiteringDelay(loiteringDelay)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
            
        // You might want to store notification messages associated with this geofence ID
        // in SharedPreferences or Database so the Receiver can access them.
        // For simplicity here, we'll just log or assume the receiver knows or we pass it differently?
        // Actually, passing data via PendingIntent is tricky for dynamic lists.
        // Better approach: user sends notification text. We should store it.
        // Let's use SharedPreferences for lightweight storage of "GeofenceID -> NotificationText"
        
        val prefs = context.getSharedPreferences("GeofencePrefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("${id}_enter", notifyOnEntry)
            putString("${id}_exit", notifyOnExit)
            putString("${id}_dwell", notifyOnDwell)
            apply()
        }

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                Log.d(TAG, "Geofence added: $id")
                if (!activeGeofences.contains(id)) {
                    activeGeofences.add(id)
                }
            }
            addOnFailureListener {
                Log.e(TAG, "Failed to add geofence: $id", it)
            }
        }
    }

    fun removeGeofence(id: String) {
        geofencingClient.removeGeofences(listOf(id)).run {
            addOnSuccessListener {
                Log.d(TAG, "Geofence removed: $id")
                activeGeofences.remove(id)
                
                // Keep prefs clean? Maybe not strictly necessary for this demo but good practice
                val prefs = context.getSharedPreferences("GeofencePrefs", Context.MODE_PRIVATE)
                with(prefs.edit()) {
                    remove("${id}_enter")
                    remove("${id}_exit")
                    remove("${id}_dwell")
                    apply()
                }
            }
            addOnFailureListener {
                Log.e(TAG, "Failed to remove geofence: $id", it)
            }
        }
    }
    
    fun removeAllGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent).run {
            addOnSuccessListener {
                Log.d(TAG, "All geofences removed")
                activeGeofences.clear()
            }
            addOnFailureListener {
                Log.e(TAG, "Failed to remove all geofences", it)
            }
        }
    }
}
