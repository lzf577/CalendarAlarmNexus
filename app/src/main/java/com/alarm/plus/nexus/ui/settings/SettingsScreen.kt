package com.alarm.plus.nexus.ui.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigateToAbout: () -> Unit) {
    val backgroundColor = Color(0xFFF2F2F7)
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("alarm_settings", android.content.Context.MODE_PRIVATE)

    // States for the switches
    var volumeFadeIn by remember { mutableStateOf(prefs.getBoolean("volume_fade_in", true)) }
    var ringInSilentMode by remember { mutableStateOf(prefs.getBoolean("ring_in_silent_mode", true)) }
    var timerVibrate by remember { mutableStateOf(prefs.getBoolean("timer_vibrate", true)) }

    var alarmDuration by remember { mutableStateOf(prefs.getInt("alarm_duration", 5)) }
    var showDurationDialog by remember { mutableStateOf(false) }

    // 计时器铃声
    var timerRingtoneUri by remember { mutableStateOf(prefs.getString("timer_ringtone_uri", null)) }
    val timerRingtoneName by remember(timerRingtoneUri) {
        derivedStateOf {
            getTimerRingtoneName(context, timerRingtoneUri)
        }
    }
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data
                ?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            val uriStr = uri?.toString()
            timerRingtoneUri = uriStr
            prefs.edit().putString("timer_ringtone_uri", uriStr).apply()
        }
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("设置", fontSize = 20.sp, color = Color(0xFF1C1C1E)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color(0xFF1C1C1E))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // 闹钟类别
            item {
                CategoryTitle("闹钟")
                SettingsGroup {
                    val durationText = if (alarmDuration == 0) "不限制" else "${alarmDuration} 分钟"
                    SettingItemArrow(
                        title = "闹钟持续时间", 
                        value = durationText,
                        onClick = { showDurationDialog = true }
                    )
                    HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp, modifier = Modifier.padding(start = 16.dp))
                    SettingItemSwitch(
                        title = "音量渐强",
                        subtitle = "在15秒后达到最大音量",
                        checked = volumeFadeIn,
                        onCheckedChange = { 
                            volumeFadeIn = it 
                            prefs.edit().putBoolean("volume_fade_in", it).apply()
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp, modifier = Modifier.padding(start = 16.dp))
                    SettingItemSwitch(
                        title = "静音或振动模式下响铃",
                        subtitle = "手机处于静音或振动模式时，闹钟和计时器仍会响",
                        checked = ringInSilentMode,
                        onCheckedChange = { 
                            ringInSilentMode = it 
                            prefs.edit().putBoolean("ring_in_silent_mode", it).apply()
                        }
                    )
                }
            }

            // 计时器类别
            item {
                Spacer(modifier = Modifier.height(16.dp))
                CategoryTitle("计时器")
                SettingsGroup {
                    SettingItemArrow(
                        title = "计时器铃声",
                        value = timerRingtoneName,
                        onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "选择计时器铃声")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                if (!timerRingtoneUri.isNullOrEmpty()) {
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                        Uri.parse(timerRingtoneUri)
                                    )
                                }
                            }
                            ringtonePickerLauncher.launch(intent)
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp, modifier = Modifier.padding(start = 16.dp))
                    SettingItemSwitch(
                        title = "响铃时振动",
                        checked = timerVibrate,
                        onCheckedChange = { 
                            timerVibrate = it 
                            prefs.edit().putBoolean("timer_vibrate", it).apply()
                        }
                    )
                }
            }

            // 关于类别
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsGroup {
                    SettingItemArrow(
                        title = "关于", 
                        value = "",
                        onClick = onNavigateToAbout
                    )
                }
            }
        }
    }

    if (showDurationDialog) {
        val options = listOf(1, 5, 10, 15, 30)
        AlertDialog(
            onDismissRequest = { showDurationDialog = false },
            title = { Text("闹钟持续时间") },
            text = {
                Column {
                    options.forEach { minutes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    alarmDuration = minutes
                                    prefs.edit().putInt("alarm_duration", minutes).apply()
                                    showDurationDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = alarmDuration == minutes,
                                onClick = {
                                    alarmDuration = minutes
                                    prefs.edit().putInt("alarm_duration", minutes).apply()
                                    showDurationDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$minutes 分钟")
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                alarmDuration = 0
                                prefs.edit().putInt("alarm_duration", 0).apply()
                                showDurationDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = alarmDuration == 0,
                            onClick = {
                                alarmDuration = 0
                                prefs.edit().putInt("alarm_duration", 0).apply()
                                showDurationDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("不限制")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDurationDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun CategoryTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = Color(0xFF8E8E93),
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        content()
    }
}

@Composable
fun SettingItemArrow(title: String, subtitle: String? = null, value: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, fontSize = 16.sp, color = Color(0xFF1C1C1E))
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, fontSize = 12.sp, color = Color(0xFF8E8E93), lineHeight = 16.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value.isNotEmpty()) {
                Text(value, fontSize = 14.sp, color = Color(0xFF8E8E93))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF8E8E93))
        }
    }
}

@Composable
fun SettingItemSwitch(title: String, subtitle: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, fontSize = 16.sp, color = Color(0xFF1C1C1E))
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, fontSize = 12.sp, color = Color(0xFF8E8E93), lineHeight = 16.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF007AFF),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFD1D1D6)
            )
        )
    }
}

/** 根据 URI 字符串获取铃声显示名称，无 URI 时返回系统默认闹钟铃声名 */
fun getTimerRingtoneName(context: android.content.Context, uriStr: String?): String {
    return try {
        val uri = if (!uriStr.isNullOrEmpty()) {
            Uri.parse(uriStr)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
        RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "默认"
    } catch (e: Exception) {
        "默认"
    }
}
