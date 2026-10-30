package com.alarm.plus.nexus.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.alarm.plus.nexus.core.service.RingtoneService
import com.alarm.plus.nexus.core.manager.AlarmScheduler
import com.alarm.plus.nexus.data.local.AlarmEntity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        Log.d("AlarmReceiver", "Action: $action, Alarm triggered! ID: $alarmId")

        when (action) {
            "ACTION_DISMISS" -> {
                val serviceIntent = Intent(context, RingtoneService::class.java)
                context.stopService(serviceIntent)
            }
            "ACTION_SNOOZE" -> {
                val serviceIntent = Intent(context, RingtoneService::class.java)
                context.stopService(serviceIntent)
                if (alarmId != -1) {
                    val scheduler = AlarmScheduler(context)
                    // 贪睡 5 分钟 (300000 毫秒)
                    val snoozeTime = System.currentTimeMillis() + 5 * 60 * 1000
                    val snoozeAlarm = AlarmEntity(id = alarmId, timeInMillis = snoozeTime, label = "再响")
                    scheduler.schedule(snoozeAlarm)
                }
            }
            else -> {
                if (alarmId != -1) {
                    com.alarm.plus.nexus.core.manager.WakeLockManager.acquire(context)
                    
                    val serviceIntent = Intent(context, RingtoneService::class.java).apply {
                        putExtra("ALARM_ID", alarmId)
                        putExtra("IS_VIBRATE", intent.getBooleanExtra("IS_VIBRATE", true))
                        putExtra("RINGTONE_URI", intent.getStringExtra("RINGTONE_URI"))
                        putExtra("LABEL", intent.getStringExtra("LABEL"))
                    }
                    try {
                        context.startForegroundService(serviceIntent)
                    } catch (e: Exception) {
                        Log.e("AlarmReceiver", "Failed to start RingtoneService", e)
                        // Fallback to normal startService if startForegroundService fails
                        try {
                            context.startService(serviceIntent)
                        } catch (e2: Exception) {
                            Log.e("AlarmReceiver", "Failed to startService as fallback", e2)
                        }
                    }
                }
            }
        }
    }
}
