package com.alarm.plus.nexus.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alarm.plus.nexus.core.manager.AlarmScheduler
import com.alarm.plus.nexus.data.local.AlarmEntity
import com.alarm.plus.nexus.data.local.AppDatabase
import com.alarm.plus.nexus.data.repository.AlarmRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlarmRepository
    private val alarmScheduler: AlarmScheduler
    private val prefs = application.getSharedPreferences("stopwatch_prefs", android.content.Context.MODE_PRIVATE)
    private val timerPrefs = application.getSharedPreferences("timer_prefs", android.content.Context.MODE_PRIVATE)
    private var timerJob: Job? = null  // 计时器自然结束协程，reset/pause 时取消

    init {
        val alarmDao = AppDatabase.getDatabase(application).alarmDao()
        repository = AlarmRepository(alarmDao)
        alarmScheduler = AlarmScheduler(application)
    }

    val alarms: StateFlow<List<AlarmEntity>> = repository.allAlarms.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Stopwatch State
    private val _isStopwatchRunning = MutableStateFlow(prefs.getBoolean("is_running", false))
    val isStopwatchRunning = _isStopwatchRunning.asStateFlow()

    private val _stopwatchStartTime = MutableStateFlow(prefs.getLong("start_time", 0L))
    val stopwatchStartTime = _stopwatchStartTime.asStateFlow()

    private val _stopwatchElapsedBeforeStart = MutableStateFlow(prefs.getLong("elapsed_before", 0L))
    val stopwatchElapsedBeforeStart = _stopwatchElapsedBeforeStart.asStateFlow()

    private val _stopwatchLaps = MutableStateFlow(
        prefs.getString("laps", "")?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
    )
    val stopwatchLaps = _stopwatchLaps.asStateFlow()

    private fun saveStopwatchState() {
        prefs.edit().apply {
            putBoolean("is_running", _isStopwatchRunning.value)
            putLong("start_time", _stopwatchStartTime.value)
            putLong("elapsed_before", _stopwatchElapsedBeforeStart.value)
            putString("laps", _stopwatchLaps.value.joinToString(","))
            apply()
        }
    }

    fun startStopwatch() {
        if (!_isStopwatchRunning.value) {
            _stopwatchStartTime.value = System.currentTimeMillis()
            _isStopwatchRunning.value = true
            saveStopwatchState()
        }
    }

    fun pauseStopwatch() {
        if (_isStopwatchRunning.value) {
            val now = System.currentTimeMillis()
            _stopwatchElapsedBeforeStart.value += (now - _stopwatchStartTime.value)
            _isStopwatchRunning.value = false
            saveStopwatchState()
        }
    }

    fun resetStopwatch() {
        _isStopwatchRunning.value = false
        _stopwatchStartTime.value = 0L
        _stopwatchElapsedBeforeStart.value = 0L
        _stopwatchLaps.value = emptyList()
        saveStopwatchState()
    }

    fun addStopwatchLap() {
        val currentElapsed = _stopwatchElapsedBeforeStart.value + 
            if (_isStopwatchRunning.value) (System.currentTimeMillis() - _stopwatchStartTime.value) else 0L
        val currentLaps = _stopwatchLaps.value.toMutableList()
        currentLaps.add(currentElapsed)
        _stopwatchLaps.value = currentLaps
        saveStopwatchState()
    }

    // Timer State
    private val _isTimerRunning = MutableStateFlow(timerPrefs.getBoolean("is_running", false))
    val isTimerRunning = _isTimerRunning.asStateFlow()

    private val _timerStartTime = MutableStateFlow(timerPrefs.getLong("start_time", 0L))
    val timerStartTime = _timerStartTime.asStateFlow()

    private val _timerElapsedBeforeStart = MutableStateFlow(timerPrefs.getLong("elapsed_before", 0L))
    val timerElapsedBeforeStart = _timerElapsedBeforeStart.asStateFlow()

    private val _timerTotalTime = MutableStateFlow(timerPrefs.getLong("total_time", 60000L)) // Default 1 min
    val timerTotalTime = _timerTotalTime.asStateFlow()

    // id|label|timeMillis;...
    private val defaultPresetsStr = "1|跳绳|60000;2|刷牙|180000;3|溏心蛋|300000;4|煮蛋|600000;5|午休|1800000"
    private val _timerPresets = MutableStateFlow(
        timerPrefs.getString("presets", defaultPresetsStr) ?: defaultPresetsStr
    )
    val timerPresets = _timerPresets.asStateFlow()

    private val _timerSelectionMode = MutableStateFlow(false)
    val timerSelectionMode = _timerSelectionMode.asStateFlow()

    private val _timerSelectedPresetIds = MutableStateFlow(setOf<Int>())
    val timerSelectedPresetIds = _timerSelectedPresetIds.asStateFlow()

    // 计时器自然结束事件（区别于用户主动重置）
    private val _timerFinished = MutableStateFlow(false)
    val timerFinished = _timerFinished.asStateFlow()

    fun setTimerFinished() {
        _timerFinished.value = true
    }

    fun clearTimerFinished() {
        _timerFinished.value = false
    }

    private fun saveTimerState() {
        timerPrefs.edit().apply {
            putBoolean("is_running", _isTimerRunning.value)
            putLong("start_time", _timerStartTime.value)
            putLong("elapsed_before", _timerElapsedBeforeStart.value)
            putLong("total_time", _timerTotalTime.value)
            putString("presets", _timerPresets.value)
            apply()
        }
    }

    fun setTimerTotalTime(timeMillis: Long) {
        timerJob?.cancel()
        timerJob = null
        _timerFinished.value = false
        _isTimerRunning.value = false
        _timerStartTime.value = 0L
        _timerElapsedBeforeStart.value = 0L
        _timerTotalTime.value = timeMillis
        saveTimerState()
    }

    fun startTimer() {
        if (!_isTimerRunning.value) {
            val remaining = _timerTotalTime.value - _timerElapsedBeforeStart.value
            if (remaining > 0) {
                _timerStartTime.value = System.currentTimeMillis()
                _isTimerRunning.value = true
                saveTimerState()
                // 启动后台协程：精确等待剩余时长后触发完成事件
                timerJob?.cancel()
                timerJob = viewModelScope.launch {
                    delay(remaining)
                    if (_isTimerRunning.value) {  // 确认未被用户取消
                        _timerFinished.value = true
                        resetTimer()
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        if (_isTimerRunning.value) {
            val now = System.currentTimeMillis()
            _timerElapsedBeforeStart.value += (now - _timerStartTime.value)
            _isTimerRunning.value = false
            saveTimerState()
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        timerJob = null
        _isTimerRunning.value = false
        _timerStartTime.value = 0L
        _timerElapsedBeforeStart.value = 0L
        saveTimerState()
    }

    fun addTimerPreset(id: Int, label: String, timeMillis: Long) {
        val currentStr = _timerPresets.value
        val newPresetStr = "$id|$label|$timeMillis"
        _timerPresets.value = if (currentStr.isEmpty()) newPresetStr else "$currentStr;$newPresetStr"
        saveTimerState()
    }

    fun setTimerSelectionMode(enabled: Boolean) {
        _timerSelectionMode.value = enabled
        if (!enabled) {
            _timerSelectedPresetIds.value = emptySet()
        }
    }

    fun toggleTimerPresetSelection(id: Int) {
        val current = _timerSelectedPresetIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _timerSelectedPresetIds.value = current
        if (current.isEmpty()) {
            _timerSelectionMode.value = false
        }
    }

    fun selectAllTimerPresets(allIds: List<Int>) {
        if (_timerSelectedPresetIds.value.size == allIds.size) {
            _timerSelectedPresetIds.value = emptySet()
            _timerSelectionMode.value = false
        } else {
            _timerSelectedPresetIds.value = allIds.toSet()
        }
    }

    fun deleteSelectedTimerPresets() {
        val selectedIds = _timerSelectedPresetIds.value
        if (selectedIds.isEmpty()) return
        
        val currentPresets = _timerPresets.value.split(";").filter { it.isNotEmpty() }
        val updatedPresets = currentPresets.filter {
            val parts = it.split("|")
            val id = parts.getOrNull(0)?.toIntOrNull() ?: -1
            !selectedIds.contains(id)
        }
        
        _timerPresets.value = updatedPresets.joinToString(";")
        saveTimerState()
        setTimerSelectionMode(false)
    }

    fun updateTimerPreset(id: Int, label: String, timeMillis: Long) {
        val currentPresets = _timerPresets.value.split(";").filter { it.isNotEmpty() }
        val updatedPresets = currentPresets.map {
            val parts = it.split("|")
            val currentId = parts.getOrNull(0)?.toIntOrNull() ?: -1
            if (currentId == id) {
                "$id|$label|$timeMillis"
            } else {
                it
            }
        }
        _timerPresets.value = updatedPresets.joinToString(";")
        saveTimerState()
        setTimerSelectionMode(false)
    }


    fun addAlarm(timeInMillis: Long, repeatDays: String, label: String, isVibrate: Boolean, ringtoneUri: String) {
        viewModelScope.launch {
            var finalTime = timeInMillis
            if (finalTime <= System.currentTimeMillis() && repeatDays.isEmpty()) {
                val calendar = Calendar.getInstance().apply { setTimeInMillis(finalTime) }
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                finalTime = calendar.timeInMillis
            }

            val newAlarm = AlarmEntity(
                timeInMillis = finalTime,
                isEnabled = true,
                label = label,
                repeatDays = repeatDays,
                isVibrate = isVibrate,
                ringtoneUri = ringtoneUri
            )
            val id = repository.insert(newAlarm)
            val savedAlarm = newAlarm.copy(id = id.toInt())
            alarmScheduler.schedule(savedAlarm)
        }
    }

    fun updateAlarm(id: Int, timeInMillis: Long, repeatDays: String, label: String, isVibrate: Boolean, ringtoneUri: String) {
        viewModelScope.launch {
            var finalTime = timeInMillis
            if (finalTime <= System.currentTimeMillis() && repeatDays.isEmpty()) {
                val calendar = Calendar.getInstance().apply { setTimeInMillis(finalTime) }
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                finalTime = calendar.timeInMillis
            }

            val updatedAlarm = AlarmEntity(
                id = id,
                timeInMillis = finalTime,
                isEnabled = true,
                label = label,
                repeatDays = repeatDays,
                isVibrate = isVibrate,
                ringtoneUri = ringtoneUri
            )
            repository.update(updatedAlarm)
            alarmScheduler.schedule(updatedAlarm)
        }
    }

    fun toggleAlarm(alarm: AlarmEntity, isEnabled: Boolean) {
        viewModelScope.launch {
            val updatedAlarm = alarm.copy(isEnabled = isEnabled)
            repository.update(updatedAlarm)
            
            if (isEnabled) {
                // 如果开启且时间已过，自动顺延到明天（对于单次闹钟）
                var finalAlarm = updatedAlarm
                if (finalAlarm.timeInMillis <= System.currentTimeMillis() && finalAlarm.repeatDays.isEmpty()) {
                    val calendar = Calendar.getInstance().apply { setTimeInMillis(finalAlarm.timeInMillis) }
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    finalAlarm = finalAlarm.copy(timeInMillis = calendar.timeInMillis)
                    repository.update(finalAlarm)
                }
                alarmScheduler.schedule(finalAlarm)
            } else {
                alarmScheduler.cancel(alarm)
            }
        }
    }

    fun deleteAlarms(alarmIds: Set<Int>) {
        viewModelScope.launch {
            alarms.value.filter { it.id in alarmIds }.forEach { alarm ->
                alarmScheduler.cancel(alarm)
                repository.delete(alarm)
            }
        }
    }
}
