package com.alarm.plus.nexus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timeInMillis: Long, // 精确到秒的时间戳
    val isEnabled: Boolean = true,
    val label: String = "",
    val repeatDays: String = "", // 比如 "1,2,3,4,5" 代表周一到周五
    val ringtoneUri: String = "",
    val isVibrate: Boolean = true
)
