package com.example.secqraise.Others

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class BatteryStatus(applicationContext : Context) {

    private val batteryStatus : Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let {
        applicationContext.registerReceiver(null,it)
    }

    fun getBatteryPercentage(): Float {
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return level / scale.toFloat() * 100
    }

    fun isCharging(): Boolean {
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

}