package com.alarm.plus.nexus.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmScreen(
    viewModel: AlarmViewModel,
    alarmId: Int? = null,
    onBack: () -> Unit
) {
    val backgroundColor = Color(0xFFF2F2F7)

    val alarms by viewModel.alarms.collectAsState()
    val alarmToEdit = remember(alarmId, alarms) { alarms.find { it.id == alarmId } }
    
    val initialTime = alarmToEdit?.timeInMillis ?: System.currentTimeMillis()
    val calendar = Calendar.getInstance().apply { setTimeInMillis(initialTime) }

    // Wheel Picker States
    var selectedYear by remember(alarmToEdit) { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember(alarmToEdit) { mutableStateOf(calendar.get(Calendar.MONTH) + 1) }
    var selectedDay by remember(alarmToEdit) { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var selectedHour by remember(alarmToEdit) { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember(alarmToEdit) { mutableStateOf(calendar.get(Calendar.MINUTE)) }
    var selectedSecond by remember(alarmToEdit) { mutableStateOf(calendar.get(Calendar.SECOND)) }
    
    // settings
    var vibrateEnabled by remember(alarmToEdit) { mutableStateOf(alarmToEdit?.isVibrate ?: true) }
    var label by remember(alarmToEdit) { mutableStateOf(alarmToEdit?.label ?: "") }
    var ringtoneUri by remember(alarmToEdit) { mutableStateOf(alarmToEdit?.ringtoneUri ?: "") }
    
    // Label dialog state
    var showLabelDialog by remember { mutableStateOf(false) }
    var tempLabel by remember { mutableStateOf("") }

    // Ringtone picker launcher
    val ringtonePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<android.net.Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                ringtoneUri = uri.toString()
            }
        }
    }

    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000L)
            currentTime = System.currentTimeMillis()
        }
    }

    val timeDiffText by remember(
        selectedYear, selectedMonth, selectedDay,
        selectedHour, selectedMinute, selectedSecond, currentTime
    ) {
        derivedStateOf {
            val c = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth - 1)
                set(Calendar.DAY_OF_MONTH, selectedDay)
                set(Calendar.HOUR_OF_DAY, selectedHour)
                set(Calendar.MINUTE, selectedMinute)
                set(Calendar.SECOND, selectedSecond)
                set(Calendar.MILLISECOND, 0)
            }
            val diff = c.timeInMillis - currentTime
            if (diff <= 0) {
                "设定的时间已过"
            } else {
                val seconds = (diff / 1000) % 60
                val minutes = (diff / (1000 * 60)) % 60
                val hours = (diff / (1000 * 60 * 60)) % 24
                val days = diff / (1000 * 60 * 60 * 24)
                
                val parts = mutableListOf<String>()
                if (days > 0) parts.add("${days}天")
                if (hours > 0) parts.add("${hours}小时")
                if (minutes > 0) parts.add("${minutes}分钟")
                parts.add("${seconds}秒")
                
                "闹钟在 ${parts.joinToString("")} 后提醒"
            }
        }
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text(if (alarmId != null) "编辑闹钟" else "新建闹钟", fontSize = 20.sp, color = Color(0xFF1C1C1E)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color(0xFF1C1C1E))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // 组装时间
                        val c = Calendar.getInstance().apply {
                            set(Calendar.YEAR, selectedYear)
                            set(Calendar.MONTH, selectedMonth - 1)
                            set(Calendar.DAY_OF_MONTH, selectedDay)
                            set(Calendar.HOUR_OF_DAY, selectedHour)
                            set(Calendar.MINUTE, selectedMinute)
                            set(Calendar.SECOND, selectedSecond)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val repeatDays = "" // 固定单次
                        if (alarmId != null) {
                            viewModel.updateAlarm(
                                id = alarmId,
                                timeInMillis = c.timeInMillis,
                                repeatDays = repeatDays,
                                label = label.ifEmpty { "闹钟" },
                                isVibrate = vibrateEnabled,
                                ringtoneUri = ringtoneUri
                            )
                        } else {
                            viewModel.addAlarm(
                                timeInMillis = c.timeInMillis,
                                repeatDays = repeatDays,
                                label = label.ifEmpty { "闹钟" },
                                isVibrate = vibrateEnabled,
                                ringtoneUri = ringtoneUri
                            )
                        }
                        onBack()
                    }) {
                        Icon(Icons.Default.Check, "保存", tint = Color(0xFF1C1C1E))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                Text(
                    text = timeDiffText,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Color(0xFF8E8E93),
                    fontSize = 13.sp
                )
            }

            item {
                // 两排滚轮，第一排日期，第二排时间
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 第一排：日期 (年、月、日)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WheelPicker(
                            items = (2020..2050).toList(),
                            initialItem = selectedYear,
                            onItemSelected = { selectedYear = it },
                            modifier = Modifier.weight(1.2f),
                            label = "年"
                        )
                        WheelPicker(
                            items = (1..12).toList(),
                            initialItem = selectedMonth,
                            onItemSelected = { selectedMonth = it },
                            modifier = Modifier.weight(1f),
                            label = "月",
                            format = { String.format("%02d", it) }
                        )
                        WheelPicker(
                            items = (1..31).toList(), // 简化版，实际应根据年月动态计算天数
                            initialItem = selectedDay,
                            onItemSelected = { selectedDay = it },
                            modifier = Modifier.weight(1f),
                            label = "日",
                            format = { String.format("%02d", it) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 第二排：时间 (时、分、秒)
                    Row(
                        modifier = Modifier.fillMaxWidth(0.8f).height(160.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WheelPicker(
                            items = (0..23).toList(),
                            initialItem = selectedHour,
                            onItemSelected = { selectedHour = it },
                            modifier = Modifier.weight(1f),
                            format = { String.format("%02d", it) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 32.sp)
                        )
                        Text(":", fontSize = 32.sp, color = Color(0xFF007AFF), modifier = Modifier.padding(horizontal = 8.dp))
                        WheelPicker(
                            items = (0..59).toList(),
                            initialItem = selectedMinute,
                            onItemSelected = { selectedMinute = it },
                            modifier = Modifier.weight(1f),
                            format = { String.format("%02d", it) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 32.sp)
                        )
                        Text(":", fontSize = 32.sp, color = Color(0xFF007AFF), modifier = Modifier.padding(horizontal = 8.dp))
                        WheelPicker(
                            items = (0..59).toList(),
                            initialItem = selectedSecond,
                            onItemSelected = { selectedSecond = it },
                            modifier = Modifier.weight(1f),
                            format = { String.format("%02d", it) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 32.sp)
                        )
                    }
                }
            }

            // 设置项列表
            item { 
                SettingItemArrow(
                    title = "闹钟铃声", 
                    value = if (ringtoneUri.isEmpty()) "系统默认" else "已选择",
                    onClick = {
                        val intent = android.content.Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM)
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            if (ringtoneUri.isNotEmpty()) {
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, android.net.Uri.parse(ringtoneUri))
                            }
                        }
                        ringtonePickerLauncher.launch(intent)
                    }
                ) 
            }
            item { 
                SettingItemSwitch(
                    title = "响铃时振动",
                    checked = vibrateEnabled,
                    onCheckedChange = { vibrateEnabled = it }
                ) 
            }
            item { 
                SettingItemArrow(
                    title = "标签", 
                    value = label.ifEmpty { "无" },
                    onClick = {
                        tempLabel = label
                        showLabelDialog = true
                    }
                ) 
            }
        }
    }

    if (showLabelDialog) {
        AlertDialog(
            onDismissRequest = { showLabelDialog = false },
            title = { Text("标签") },
            text = {
                OutlinedTextField(
                    value = tempLabel,
                    onValueChange = { tempLabel = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    label = tempLabel
                    showLabelDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLabelDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun RepeatChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                color = if (selected) Color(0xFF007AFF) else Color(0xFFE5E5EA),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFF8E8E93),
            fontSize = 14.sp
        )
    }
}

@Composable
fun SettingItemArrow(title: String, value: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 16.sp, color = Color(0xFF1C1C1E))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontSize = 14.sp, color = Color(0xFF8E8E93))
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
            .padding(horizontal = 24.dp, vertical = 12.dp),
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    items: List<T>,
    initialItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    format: (T) -> String = { it.toString() },
    textStyle: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp)
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = items.indexOf(initialItem))
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    
    // 监听中心元素变化
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                // paddedItems 形式为 [null, item0, item1, ..., null]
                // 屏幕显示3项，第一项(top)索引为 index，中间项索引为 index+1
                // paddedItems[index+1] 实际上就是 items[index]
                if (index in items.indices) {
                    onItemSelected(items[index])
                }
            }
    }

    // 前后填充空元素以允许滚动到头尾
    val paddedItems = remember(items) { listOf(null) + items + listOf(null) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            modifier = Modifier.height(150.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(paddedItems.size) { index ->
                val item = paddedItems[index]
                val isCenter = index == listState.firstVisibleItemIndex + 1
                Box(
                    modifier = Modifier.height(50.dp).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (item != null) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = format(item),
                                style = textStyle,
                                fontWeight = if (isCenter) FontWeight.Normal else FontWeight.Light,
                                color = if (isCenter) Color(0xFF007AFF) else Color(0xFFAEAEB2),
                                modifier = Modifier.alpha(if (isCenter) 1f else 0.5f)
                            )
                            if (label.isNotEmpty() && isCenter) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    color = Color(0xFF007AFF),
                                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
