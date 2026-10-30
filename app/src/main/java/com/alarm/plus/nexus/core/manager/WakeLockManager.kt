package com.alarm.plus.nexus.core.manager

import android.content.Context
import android.os.PowerManager

object WakeLockManager {
    private var wakeLock: PowerManager.WakeLock? = null

    fun acquire(context: Context) {
        if (wakeLock == null) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "CalendarAlarmNexus:AlarmWakeLock"
            )
            // The wake lock is reference counted by default
            wakeLock?.setReferenceCounted(false)
        }
        // Acquire for a maximum of 10 minutes to prevent infinite battery drain in case of crash
        wakeLock?.acquire(10 * 60 * 1000L)
    }

    fun release() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
