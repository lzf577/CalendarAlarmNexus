package com.alarm.plus.nexus.data.repository

import com.alarm.plus.nexus.data.local.AlarmDao
import com.alarm.plus.nexus.data.local.AlarmEntity
import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val alarmDao: AlarmDao) {

    val allAlarms: Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()

    suspend fun insert(alarm: AlarmEntity): Long {
        return alarmDao.insert(alarm)
    }

    suspend fun update(alarm: AlarmEntity) {
        alarmDao.update(alarm)
    }

    suspend fun delete(alarm: AlarmEntity) {
        alarmDao.delete(alarm)
    }
}
