package com.alarm.plus.nexus.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device rebooted. Need to reschedule all alarms.")
            // 在实际项目中，这里需要启动协程从 Room 读取所有启用的闹钟，然后调用 AlarmScheduler
        }
    }
}
