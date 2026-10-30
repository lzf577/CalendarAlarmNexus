package com.alarm.plus.nexus.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.alarm.plus.nexus.MainActivity
import com.alarm.plus.nexus.core.receiver.AlarmReceiver

class RingtoneService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var volumeAnimator: android.animation.ValueAnimator? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        val isTimer = intent?.getBooleanExtra("IS_TIMER", false) ?: false
        val label = intent?.getStringExtra("LABEL") ?: ""

        val prefs = getSharedPreferences("alarm_settings", Context.MODE_PRIVATE)
        val volumeFadeIn = prefs.getBoolean("volume_fade_in", true)
        val ringInSilentMode = prefs.getBoolean("ring_in_silent_mode", true)

        // 根据是闹钟还是计时器，分别读取铃声 URI 和震动设置
        val ringtoneUriStr: String?
        val isVibrate: Boolean
        if (isTimer) {
            ringtoneUriStr = prefs.getString("timer_ringtone_uri", null)
            isVibrate = prefs.getBoolean("timer_vibrate", true)
        } else {
            ringtoneUriStr = intent?.getStringExtra("RINGTONE_URI")
            isVibrate = intent?.getBooleanExtra("IS_VIBRATE", true) ?: true
        }

        val displayLabel = when {
            isTimer -> if (label.isEmpty()) "计时器结束" else label
            else -> if (label.isEmpty()) "闹钟响了！" else label
        }
        
        createNotificationChannel()

        val dismissIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = "ACTION_DISMISS"
            putExtra("ALARM_ID", alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            if (alarmId >= 0) alarmId * 2 else 9999,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = if (isTimer) {
            // 计时器结束：普通浮动通知，不弹出全屏界面
            val tapIntent = Intent(this, com.alarm.plus.nexus.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val tapPendingIntent = PendingIntent.getActivity(
                this, 8888, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Builder(this, "ALARM_CHANNEL_ID")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("日历闹钟 Nexus")
                .setContentText(displayLabel)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(tapPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭", dismissPendingIntent)
                .setAutoCancel(false)
                .build()
        } else {
            // 闹钟：全屏唤醒界面
            val fullScreenIntent = Intent(this, com.alarm.plus.nexus.AlarmRingingActivity::class.java).apply {
                putExtra("ALARM_ID", alarmId)
                putExtra("LABEL", displayLabel)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                this, 0, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try { startActivity(fullScreenIntent) } catch (e: Exception) { e.printStackTrace() }

            val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
                action = "ACTION_SNOOZE"
                putExtra("ALARM_ID", alarmId)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                this, alarmId * 2 + 1, snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Builder(this, "ALARM_CHANNEL_ID")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("日历闹钟 Nexus")
                .setContentText(displayLabel)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭", dismissPendingIntent)
                .addAction(android.R.drawable.ic_menu_recent_history, "再响", snoozePendingIntent)
                .build()
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val alarmUri = if (!ringtoneUriStr.isNullOrEmpty()) {
                android.net.Uri.parse(ringtoneUriStr)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@RingtoneService, alarmUri)
                val usage = if (ringInSilentMode) {
                    android.media.AudioAttributes.USAGE_ALARM
                } else {
                    android.media.AudioAttributes.USAGE_NOTIFICATION
                }
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(usage)
                        .build()
                )
                isLooping = true
                if (volumeFadeIn && !isTimer) {
                    setVolume(0f, 0f)
                }
                prepare()
                start()
            }

            if (volumeFadeIn && !isTimer) {
                volumeAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 15000
                    addUpdateListener { animator ->
                        val volume = animator.animatedValue as Float
                        try {
                            mediaPlayer?.setVolume(volume, volume)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    start()
                }
            }

            // 计时器模式：60秒后自动停止；闹钟模式：读取用户设定的持续时间
            val stopDelayMs = if (isTimer) {
                60 * 1000L
            } else {
                val alarmDuration = prefs.getInt("alarm_duration", 5)
                if (alarmDuration > 0) alarmDuration * 60 * 1000L else -1L
            }
            if (stopDelayMs > 0) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    stopSelf()
                }, stopDelayMs)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (isVibrate) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibrator = vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 500), 1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 1000, 500), 1)
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            volumeAnimator?.cancel()
            volumeAnimator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        vibrator = null

        try {
            com.alarm.plus.nexus.core.manager.WakeLockManager.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "ALARM_CHANNEL_ID",
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
