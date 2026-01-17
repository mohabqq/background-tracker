package com.example.background_tracker

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import com.example.background_tracker.database.LocationDatabase
import com.example.background_tracker.geofence.GeofenceManager
import com.example.background_tracker.config.ConfigManager

/** BackgroundTrackerPlugin */
class BackgroundTrackerPlugin: FlutterPlugin, MethodCallHandler, ActivityAware, RequestPermissionsResultListener {
  private lateinit var channel : MethodChannel
  private lateinit var context: Context
  private lateinit var locationDatabase: LocationDatabase
  private lateinit var geofenceManager: GeofenceManager
  private lateinit var configManager: ConfigManager
  private var activity: Activity? = null
  private var pendingResult: Result? = null
  private val PERMISSION_REQUEST_CODE = 123

  private val locationReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      android.util.Log.d("BackgroundTrackerPlugin", "Broadcast received: ${intent?.action}")
      if (intent?.action == "com.example.background_tracker.LOCATION_UPDATE") {
        val lat = intent.getDoubleExtra("latitude", 0.0)
        val lng = intent.getDoubleExtra("longitude", 0.0)
        android.util.Log.d("BackgroundTrackerPlugin", "Location: $lat, $lng")
        channel.invokeMethod("onLocation", mapOf("latitude" to lat, "longitude" to lng))
        android.util.Log.d("BackgroundTrackerPlugin", "Sent to Flutter")
      } else if (intent?.action == "com.example.background_tracker.GEOFENCE_EVENT") {
        val id = intent.getStringExtra("id")
        val transition = intent.getStringExtra("transition")
        android.util.Log.d("BackgroundTrackerPlugin", "Geofence event: $id - $transition")
        channel.invokeMethod("onGeofenceEvent", mapOf("id" to id, "transition" to transition))
      }
    }
  }

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "background_tracker")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
    locationDatabase = LocationDatabase(context)
    geofenceManager = GeofenceManager(context)
    configManager = ConfigManager(context)

    // Register receiver
    val filter = IntentFilter()
    filter.addAction("com.example.background_tracker.LOCATION_UPDATE")
    filter.addAction("com.example.background_tracker.GEOFENCE_EVENT")
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        // Use EXPORTED because LocationService (different component) sends broadcasts
        context.registerReceiver(locationReceiver, filter, Context.RECEIVER_EXPORTED)
    } else {
        context.registerReceiver(locationReceiver, filter)
    }
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "getPlatformVersion") {
      result.success("Android ${android.os.Build.VERSION.RELEASE}")
    } else if (call.method == "startTracking") {
      startService()
      result.success(true)
    } else if (call.method == "stopTracking") {
      stopService()
      result.success(true)
    } else if (call.method == "requestPermissions") {
      requestPermissions(result)
    } else if (call.method == "configure") {
      val args = call.arguments as Map<String, Any>
      configManager.saveConfig(args)
      result.success(null)
    } else if (call.method == "getLocations") {
      val limit = call.argument<Int>("limit") ?: 100
      val locations = locationDatabase.getUnsyncedLocations(limit)
      result.success(locations)
    } else if (call.method == "getLocationCount") {
      val count = locationDatabase.getLocationCount()
      result.success(count)
    } else if (call.method == "clearDatabase") {
      locationDatabase.clearAll()
      result.success(null)
    } else if (call.method == "addGeofence") {
      val args = call.arguments as Map<String, Any>
      geofenceManager.addGeofence(
        id = args["id"] as String,
        latitude = args["latitude"] as Double,
        longitude = args["longitude"] as Double,
        radius = (args["radius"] as Double).toFloat(),
        notifyOnEntry = args["notifyOnEntry"] as String?,
        notifyOnExit = args["notifyOnExit"] as String?,
        notifyOnDwell = args["notifyOnDwell"] as String?,
        loiteringDelay = args["loiteringDelay"] as Int
      )
      result.success(null)
    } else if (call.method == "removeGeofence") {
        val id = call.arguments as String
        geofenceManager.removeGeofence(id)
        result.success(null)
    } else {
      result.notImplemented()
    }
  }

  private fun requestPermissions(result: Result) {
    if (activity == null) {
      result.error("NO_ACTIVITY", "Activity is null", null)
      return
    }

    val permissions = mutableListOf(
      Manifest.permission.ACCESS_FINE_LOCATION,
      Manifest.permission.ACCESS_COARSE_LOCATION
    )

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
      permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }
    
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
    }

    pendingResult = result
    ActivityCompat.requestPermissions(activity!!, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
  }

  private fun startService() {
    val intent = Intent(context, LocationService::class.java)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
  }

  private fun stopService() {
    val intent = Intent(context, LocationService::class.java)
    context.stopService(intent)
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
    context.unregisterReceiver(locationReceiver)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
    binding.addRequestPermissionsResultListener(this)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activity = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onDetachedFromActivity() {
    activity = null
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
    if (requestCode == PERMISSION_REQUEST_CODE) {
      val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
      pendingResult?.success(allGranted)
      pendingResult = null
      return true
    }
    return false
  }
}
