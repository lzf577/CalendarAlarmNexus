package com.alarm.plus.nexus.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import android.content.Intent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.plus.nexus.core.service.RingtoneService
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

data class TimerPreset(val id: Int, val label: String, val timeMillis: Long)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimerTab(viewModel: AlarmViewModel) {
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val startTime by viewModel.timerStartTime.collectAsState()
    val elapsedBeforeStart by viewModel.timerElapsedBeforeStart.collectAsState()
    val totalTime by viewModel.timerTotalTime.collectAsState()
    val presetsStr by viewModel.timerPresets.collectAsState()
    val isSelectionMode by viewModel.timerSelectionMode.collectAsState()
    val selectedIds by viewModel.timerSelectedPresetIds.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var presetToEdit by remember { mutableStateOf<TimerPreset?>(null) }

    val presets = remember(presetsStr) {
        presetsStr.split(";").filter { it.isNotEmpty() }.mapNotNull {
            val parts = it.split("|")
            if (parts.size == 3) {
                TimerPreset(parts[0].toIntOrNull() ?: 0, parts[1], parts[2].toLongOrNull() ?: 0L)
            } else null
        }
    }

    val timerFinished by viewModel.timerFinished.collectAsState()
    val context = LocalContext.current

    // 计时器自然倒计时归零时响铃（用户主动重置不会进入此分支）
    LaunchedEffect(timerFinished) {
        if (timerFinished) {
            val serviceIntent = Intent(context, RingtoneService::class.java).apply {
                putExtra("IS_TIMER", true)
                putExtra("LABEL", "计时器结束")
                putExtra("ALARM_ID", -2)
            }
            try {
                context.startForegroundService(serviceIntent)
            } catch (e: Exception) {
                context.startService(serviceIntent)
            }
            viewModel.clearTimerFinished()
        }
    }

    var currentRemainingMillis by remember { mutableStateOf(totalTime - elapsedBeforeStart) }

    LaunchedEffect(isRunning, startTime, elapsedBeforeStart, totalTime) {
        if (isRunning) {
            while (true) {
                withFrameMillis {
                    val elapsed = elapsedBeforeStart + (System.currentTimeMillis() - startTime)
                    val remaining = totalTime - elapsed
                    // 只更新 UI 显示，结束逻辑由 ViewModel 协程负责
                    currentRemainingMillis = if (remaining > 0) remaining else 0L
                }
            }
        } else {
            val remaining = totalTime - elapsedBeforeStart
            currentRemainingMillis = if (remaining > 0) remaining else 0L
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 合并卡片：表盘 + 预设网格
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color(0x22000000))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 表盘 + 时间显示
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TimerDial(
                        remainingMillis = currentRemainingMillis,
                        totalMillis = totalTime
                    )

                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .shadow(16.dp, CircleShape, spotColor = Color(0x33000000))
                            .clip(CircleShape)
                            .background(Color(0xFFF2F2F7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatTimerTime(currentRemainingMillis),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Light,
                            color = Color(0xFF1C1C1E)
                        )
                    }
                }

                // 分隔线
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = Color(0xFFE5E5EA),
                    thickness = 0.5.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 预设网格
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    maxItemsInEachRow = 3
                ) {

            // Add custom preset button
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(80.dp)
                    .border(1.dp, Color(0xFFAEAEB2), CircleShape)
                    .clickable { showAddDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Text("+", fontSize = 36.sp, color = Color(0xFF1C1C1E), fontWeight = FontWeight.Light)
            }

            presets.forEach { preset ->
                val isSelectedForTimer = totalTime == preset.timeMillis && !isRunning && elapsedBeforeStart == 0L
                val isSelectedForAction = selectedIds.contains(preset.id)
                val isSelected = if (isSelectionMode) false else isSelectedForTimer
                
                val bgColor = if (isSelected) Color(0xFF007AFF) else Color(0xFFFFFFFF)
                val textColor = if (isSelected) Color.White else Color(0xFF1C1C1E)
                val subTextColor = if (isSelected) Color(0xCCFFFFFF) else Color(0xFF007AFF)

                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(80.dp)
                        .shadow(if (isSelected) 8.dp else 4.dp, CircleShape, spotColor = Color(0x33000000))
                        .clip(CircleShape)
                        .background(bgColor)
                        .combinedClickable(
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleTimerPresetSelection(preset.id)
                                } else {
                                    viewModel.setTimerTotalTime(preset.timeMillis)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    viewModel.setTimerSelectionMode(true)
                                    viewModel.toggleTimerPresetSelection(preset.id)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(preset.label, fontSize = 14.sp, color = textColor)
                        Text(formatPresetTime(preset.timeMillis), fontSize = 12.sp, color = subTextColor)
                    }
                    if (isSelectionMode) {
                        if (isSelectedForAction) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "已选择",
                                tint = Color(0xFF007AFF),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = 16.dp, y = 4.dp)
                                    .size(24.dp)
                                    .background(Color.White, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = 16.dp, y = 4.dp)
                                    .size(24.dp)
                                    .background(Color.White, CircleShape)
                                    .border(1.5.dp, Color(0xFFAEAEB2), CircleShape)
                            )
                        }
                    }
                }
            }
        }
            } // closes inner Column
        }     // closes card Box

        // Bottom Controls

        if (isSelectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 64.dp, top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(enabled = selectedIds.size == 1) {
                            presetToEdit = presets.find { it.id == selectedIds.first() }
                            showAddDialog = true
                        }
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = if (selectedIds.size == 1) Color(0xFF1C1C1E) else Color(0xFFAEAEB2),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "编辑",
                        fontSize = 12.sp,
                        color = if (selectedIds.size == 1) Color(0xFF1C1C1E) else Color(0xFFAEAEB2),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(enabled = selectedIds.isNotEmpty()) {
                            viewModel.deleteSelectedTimerPresets()
                        }
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = if (selectedIds.isNotEmpty()) Color(0xFF1C1C1E) else Color(0xFFAEAEB2),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "删除",
                        fontSize = 12.sp,
                        color = if (selectedIds.isNotEmpty()) Color(0xFF1C1C1E) else Color(0xFFAEAEB2),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 64.dp, top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Button: Play / Pause
                val leftButtonColor = Color(0xFF0055CC)
                FloatingActionButton(
                    onClick = {
                        if (isRunning) {
                            viewModel.pauseTimer()
                        } else if (currentRemainingMillis > 0) {
                            viewModel.startTimer()
                        }
                    },
                    shape = CircleShape,
                    containerColor = leftButtonColor,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                    modifier = Modifier.size(72.dp)
                ) {
                    if (isRunning) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp, 20.dp).background(Color.White))
                            Box(modifier = Modifier.size(6.dp, 20.dp).background(Color.White))
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "开始",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Right Button: Refresh
                FloatingActionButton(
                    onClick = {
                        viewModel.clearTimerFinished()
                        viewModel.resetTimer()
                        // 用户主动重置：停止正在播放的铃声（如有）
                        try { context.stopService(Intent(context, RingtoneService::class.java)) } catch (e: Exception) {}
                    },
                    shape = CircleShape,
                    containerColor = Color(0xFF4A90D9),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重置",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddTimerPresetDialog(
            preset = presetToEdit,
            onDismiss = { 
                showAddDialog = false
                presetToEdit = null
            },
            onConfirm = { h, m, s, label ->
                val timeMillis = (h * 3600 + m * 60 + s) * 1000L
                if (timeMillis > 0) {
                    if (presetToEdit != null) {
                        viewModel.updateTimerPreset(presetToEdit!!.id, label, timeMillis)
                    } else {
                        val newId = (presets.maxOfOrNull { it.id } ?: 0) + 1
                        viewModel.addTimerPreset(newId, label, timeMillis)
                    }
                    viewModel.setTimerTotalTime(timeMillis)
                }
                showAddDialog = false
                presetToEdit = null
            }
        )
    }
}

@Composable
fun TimerDial(remainingMillis: Long, totalMillis: Long) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f * 0.9f

        // Draw 4 ticks
        for (i in 0 until 4) {
            val angle = Math.toRadians((i * 90 - 90).toDouble())
            val startDist = radius + 8.dp.toPx()
            val endDist = radius + 16.dp.toPx()
            drawLine(
                color = Color(0xFF1C1C1E),
                start = Offset(
                    (center.x + startDist * cos(angle)).toFloat(),
                    (center.y + startDist * sin(angle)).toFloat()
                ),
                end = Offset(
                    (center.x + endDist * cos(angle)).toFloat(),
                    (center.y + endDist * sin(angle)).toFloat()
                ),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }

        // Draw arc progress
        val progress = if (totalMillis > 0) (remainingMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f) else 0f
        val sweepAngle = progress * 360f
        val startAngle = -90f

        drawArc(
            color = Color(0xFF007AFF),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )

        // Draw indicator dot at the end of the arc
        if (progress > 0f) {
            val indicatorAngle = Math.toRadians((startAngle + sweepAngle).toDouble())
            val indicatorCenter = Offset(
                (center.x + radius * cos(indicatorAngle)).toFloat(),
                (center.y + radius * sin(indicatorAngle)).toFloat()
            )
            drawCircle(
                color = Color(0xFF007AFF),
                radius = 10.dp.toPx(),
                center = indicatorCenter
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimerPresetDialog(
    preset: TimerPreset? = null,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int, String) -> Unit
) {
    val initialTime = preset?.timeMillis ?: 0L
    val initialSeconds = (initialTime / 1000).toInt()
    
    var hours by remember { mutableStateOf(initialSeconds / 3600) }
    var minutes by remember { mutableStateOf((initialSeconds % 3600) / 60) }
    var seconds by remember { mutableStateOf(initialSeconds % 60) }
    var label by remember { mutableStateOf(preset?.label ?: "自定义") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("预设时间", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelPicker(
                        items = (0..99).toList(),
                        initialItem = hours,
                        onItemSelected = { hours = it },
                        modifier = Modifier.weight(1f),
                        format = { String.format("%02d", it) }
                    )
                    Text(":", fontSize = 24.sp, color = Color(0xFF007AFF), modifier = Modifier.padding(horizontal = 4.dp))
                    WheelPicker(
                        items = (0..59).toList(),
                        initialItem = minutes,
                        onItemSelected = { minutes = it },
                        modifier = Modifier.weight(1f),
                        format = { String.format("%02d", it) }
                    )
                    Text(":", fontSize = 24.sp, color = Color(0xFF007AFF), modifier = Modifier.padding(horizontal = 4.dp))
                    WheelPicker(
                        items = (0..59).toList(),
                        initialItem = seconds,
                        onItemSelected = { seconds = it },
                        modifier = Modifier.weight(1f),
                        format = { String.format("%02d", it) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("标签") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hours, minutes, seconds, label) }) {
                Text("确定", color = Color(0xFF007AFF))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF8E8E93))
            }
        }
    )
}

fun formatTimerTime(millis: Long): String {
    val totalSeconds = (millis + 999) / 1000 // Round up for timer display
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

fun formatPresetTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    val parts = mutableListOf<String>()
    if (hours > 0) parts.add("${hours}小时")
    if (minutes > 0) parts.add("${minutes}分钟")
    if (seconds > 0 || parts.isEmpty()) parts.add("${seconds}秒")
    return parts.joinToString("")
}
