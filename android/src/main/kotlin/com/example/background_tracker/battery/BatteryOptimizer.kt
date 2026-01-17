package com.example.background_tracker.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log

/**
 * Monitors battery state and optimizes tracking parameters
 */
class BatteryOptimizer(
    private val context: Context,
    private val onOptimizationRequired: (Boolean, Boolean) -> Unit // (isLowPower, isCharging)
) {

    companion object {
        private const val TAG = "BatteryOptimizer"
        private const val LOW_BATTERY_THRESHOLD = 50 // %
        private const val CRITICAL_BATTERY_THRESHOLD = 15 // %
    }

    private var isLowPowerMode = false
    private var isCharging = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { checkBatteryState(it) }
        }
    }

    fun start() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(batteryReceiver, filter)
        // Initial check
        batteryStatus?.let { checkBatteryState(it) }
        Log.d(TAG, "Battery optimizer started")
    }

    fun stop() {
        try {
            context.unregisterReceiver(batteryReceiver)
            Log.d(TAG, "Battery optimizer stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping battery optimizer", e)
        }
    }

    private fun checkBatteryState(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = level * 100 / scale.toFloat()

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isChargingNow = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val isLowPowerNow = batteryPct <= LOW_BATTERY_THRESHOLD && !isChargingNow
        
        Log.d(TAG, "Battery: $batteryPct%, Charging: $isChargingNow, LowPower: $isLowPowerNow")

        if (isLowPowerNow != isLowPowerMode || isChargingNow != isCharging) {
            isLowPowerMode = isLowPowerNow
            isCharging = isChargingNow
            onOptimizationRequired(isLowPowerMode, isCharging)
        }
    }
    
    fun getOptimizationParams(): Pair<Boolean, Boolean> {
        return Pair(isLowPowerMode, isCharging)
    }
}
