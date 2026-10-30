package com.alarm.plus.nexus.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// 示例数据
data class AlarmData(
    val id: Int,
    val timeInMillis: Long,
    val repeatDesc: String,
    val isEnabled: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: AlarmViewModel,
    cities: List<com.alarm.plus.nexus.ui.main.WorldCity>,
    onCitiesChanged: (List<com.alarm.plus.nexus.ui.main.WorldCity>) -> Unit,
    onNavigateToAddAlarm: () -> Unit,
    onNavigateToEditAlarm: (Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAddCity: () -> Unit,
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {}
) {
    
    // 状态提升：选择模式和选中的 ID (用于闹钟和世界时钟)
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }

    // 计时器的选择状态（由 ViewModel 托管）
    val timerSelectionMode by viewModel.timerSelectionMode.collectAsState()
    val timerSelectedPresetIds by viewModel.timerSelectedPresetIds.collectAsState()
    val timerPresetsStr by viewModel.timerPresets.collectAsState()
    val timerPresetsCount = remember(timerPresetsStr) { timerPresetsStr.split(";").count { it.isNotEmpty() } }

    val currentIsSelectionMode = isSelectionMode || (selectedTab == 3 && timerSelectionMode)
    val currentSelectedIdsCount = if (selectedTab == 3) timerSelectedPresetIds.size else selectedIds.size

    // 物理返回键：选择模式下退出选择，而不是退到桌面
    BackHandler(enabled = currentIsSelectionMode) {
        if (selectedTab == 3) {
            viewModel.setTimerSelectionMode(false)
        } else {
            isSelectionMode = false
            selectedIds = emptySet()
        }
    }

    // 真实闹钟数据状态
    val alarms by viewModel.alarms.collectAsState()

    val backgroundColor = Color(0xFFF2F2F7)

    val tabTitles = listOf("闹钟", "世界时钟", "秒表", "计时器")
    val tabIcons = listOf(
        Icons.Default.List,
        Icons.Default.Place,
        Icons.Default.PlayArrow,
        Icons.Default.Refresh
    )

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            if (currentIsSelectionMode) {
                // 选择模式下的顶部栏
                TopAppBar(
                    title = {
                        Text(
                            text = "${currentSelectedIdsCount} 项已选择",
                            fontSize = 20.sp,
                            color = Color(0xFF1C1C1E)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            if (selectedTab == 3) {
                                viewModel.setTimerSelectionMode(false)
                            } else {
                                isSelectionMode = false
                                selectedIds = emptySet()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color(0xFF1C1C1E))
                        }
                    },
                    actions = {
                        val currentListSize = when (selectedTab) {
                            0 -> alarms.size
                            3 -> timerPresetsCount
                            else -> cities.size
                        }
                        val isAllSelected = currentSelectedIdsCount == currentListSize && currentListSize > 0
                        IconButton(onClick = {
                            if (isAllSelected) {
                                if (selectedTab == 3) {
                                    viewModel.selectAllTimerPresets(emptyList())
                                } else {
                                    selectedIds = emptySet()
                                }
                            } else {
                                if (selectedTab == 3) {
                                    val allIds = timerPresetsStr.split(";").filter { it.isNotEmpty() }.mapNotNull { it.split("|").getOrNull(0)?.toIntOrNull() }
                                    viewModel.selectAllTimerPresets(allIds)
                                } else {
                                    selectedIds = if (selectedTab == 0) alarms.map { it.id }.toSet() else cities.map { it.id }.toSet()
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "全选",
                                tint = if (isAllSelected) Color(0xFF007AFF) else Color(0xFF1C1C1E)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
                )
            } else {
                // 正常模式下的顶部栏
                TopAppBar(
                    title = {
                        Text(
                            text = tabTitles[selectedTab],
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E)
                        )
                    },
                    actions = {
                        if (selectedTab == 0 || selectedTab == 3) {
    
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "设置",
                                    tint = Color(0xFF1C1C1E)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
                )
            }
        },
        floatingActionButton = {
            if ((selectedTab == 0 || selectedTab == 1) && !isSelectionMode) {
                FloatingActionButton(
                    onClick = {
                        if (selectedTab == 0) onNavigateToAddAlarm()
                        else if (selectedTab == 1) onNavigateToAddCity()
                    },
                    shape = CircleShape,
                    containerColor = Color(0xFF007AFF),
                    contentColor = Color.White,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (selectedTab == 0) "添加闹钟" else "添加城市",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            if (currentIsSelectionMode && selectedTab != 3) {
                // 选择模式下的底部操作栏 (闹钟、世界时钟)
                BottomAppBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp,
                    contentPadding = PaddingValues(horizontal = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                isSelectionMode = false
                                selectedIds = emptySet()
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "取消", tint = Color(0xFF1C1C1E))
                            Text("取消", fontSize = 12.sp, color = Color(0xFF1C1C1E))
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                if (selectedTab == 0) {
                                    viewModel.deleteAlarms(selectedIds)
                                } else {
                                    onCitiesChanged(cities.filterNot { it.id in selectedIds })
                                }
                                isSelectionMode = false
                                selectedIds = emptySet()
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFF1C1C1E))
                            Text("删除", fontSize = 12.sp, color = Color(0xFF1C1C1E))
                        }
                    }
                }
            } else if (!currentIsSelectionMode) {
                // 正常模式下的导航栏
                NavigationBar(
                    containerColor = backgroundColor,
                    tonalElevation = 0.dp
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { onTabSelected(index) },
                            icon = {
                                Icon(
                                    imageVector = tabIcons[index],
                                    contentDescription = title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(text = title, fontSize = 11.sp)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF007AFF),
                                selectedTextColor = Color(0xFF007AFF),
                                unselectedIconColor = Color(0xFF8E8E93),
                                unselectedTextColor = Color(0xFF8E8E93),
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> AlarmTab(
                    alarms = alarms,
                    isSelectionMode = isSelectionMode,
                    selectedAlarmIds = selectedIds,
                    onToggleAlarm = { id, enabled ->
                        val alarm = alarms.find { it.id == id }
                        if (alarm != null) {
                            viewModel.toggleAlarm(alarm, enabled)
                        }
                    },
                    onEnterSelectionMode = { id ->
                        isSelectionMode = true
                        selectedIds = setOf(id)
                    },
                    onToggleSelection = { id ->
                        selectedIds = if (selectedIds.contains(id)) {
                            selectedIds - id
                        } else {
                            selectedIds + id
                        }
                    },
                    onEditAlarm = onNavigateToEditAlarm
                )
                1 -> WorldClockTab(
                    cities = cities,
                    isSelectionMode = isSelectionMode,
                    selectedCityIds = selectedIds,
                    onEnterSelectionMode = { id ->
                        isSelectionMode = true
                        selectedIds = setOf(id)
                    },
                    onToggleSelection = { id ->
                        selectedIds = if (selectedIds.contains(id)) {
                            selectedIds - id
                        } else {
                            selectedIds + id
                        }
                    }
                )
                2 -> StopwatchTab(viewModel = viewModel)
                3 -> TimerTab(viewModel = viewModel)
                else -> PlaceholderTab(tabTitles[selectedTab])
            }
        }
    }
}

@Composable
fun AlarmTab(
    alarms: List<com.alarm.plus.nexus.data.local.AlarmEntity>,
    isSelectionMode: Boolean,
    selectedAlarmIds: Set<Int>,
    onToggleAlarm: (Int, Boolean) -> Unit,
    onEnterSelectionMode: (Int) -> Unit,
    onToggleSelection: (Int) -> Unit,
    onEditAlarm: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            AnalogClock(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 60.dp, vertical = 24.dp)
                    .height(200.dp)
            )
        }
        items(alarms, key = { it.id }) { alarm ->
            AlarmListItem(
                alarm = alarm,
                isSelectionMode = isSelectionMode,
                isSelected = selectedAlarmIds.contains(alarm.id),
                onToggle = { onToggleAlarm(alarm.id, it) },
                onLongClick = { onEnterSelectionMode(alarm.id) },
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection(alarm.id)
                    } else {
                        onEditAlarm(alarm.id)
                    }
                }
            )
            HorizontalDivider(
                color = Color(0xFFD1D1D6),
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmListItem(
    alarm: com.alarm.plus.nexus.data.local.AlarmEntity, 
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val dateFormatter = SimpleDateFormat("MM月dd日", Locale.getDefault())

    val date = Date(alarm.timeInMillis)
    val timeStr = timeFormatter.format(date)
    val dateStr = dateFormatter.format(date)

    val textColor = if (alarm.isEnabled || isSelectionMode) Color(0xFF1C1C1E) else Color(0xFFAEAEB2)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // 日期在时间前面（核心需求）
            Text(
                text = dateStr,
                fontSize = 13.sp,
                color = Color(0xFF8E8E93),
                fontWeight = FontWeight.Normal
            )
            // 大号时间
            Text(
                text = timeStr,
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                color = textColor,
                lineHeight = 48.sp
            )
            val repeatText = alarm.repeatDays.ifEmpty { "单次" }
            val labelText = if (alarm.label.isNotEmpty() && alarm.label != "闹钟") "  ${alarm.label}" else ""
            Text(
                text = "$repeatText$labelText",
                fontSize = 13.sp,
                color = Color(0xFF8E8E93)
            )
        }
        
        Crossfade(targetState = isSelectionMode, label = "selection_mode_transition") { selectionMode ->
            if (selectionMode) {
                // 选择模式下的勾选框
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "已选择",
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    // 未选中的空心圆
                    Canvas(modifier = Modifier.size(24.dp)) {
                        drawCircle(
                            color = Color(0xFFD1D1D6),
                            radius = size.minDimension / 2,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                        )
                    }
                }
            } else {
                // 正常模式下的开关
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF007AFF), // 改为与iOS相似的蓝色
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFD1D1D6)
                    )
                )
            }
        }
    }
}

@Composable
fun AnalogClock(modifier: Modifier = Modifier) {
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTime = Calendar.getInstance()
        }
    }

    val hour = currentTime.get(Calendar.HOUR)
    val minute = currentTime.get(Calendar.MINUTE)
    val second = currentTime.get(Calendar.SECOND)

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = minOf(size.width, size.height) / 2f

        // 表盘白色背景
        drawCircle(color = Color.White, radius = radius, center = center)

        // 12个刻度点
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30 - 90).toDouble())
            val dotRadius = if (i % 3 == 0) 5f else 2.5f
            val dotDist = radius * 0.87f
            drawCircle(
                color = Color(0xFFAEAEB2),
                radius = dotRadius,
                center = Offset(
                    (center.x + dotDist * cos(angle)).toFloat(),
                    (center.y + dotDist * sin(angle)).toFloat()
                )
            )
        }

        // 时针
        val hourAngle = Math.toRadians(((hour * 30 + minute * 0.5) - 90))
        drawHand(center, hourAngle, radius * 0.5f, 6f, Color(0xFF1C1C1E))

        // 分针
        val minuteAngle = Math.toRadians((minute * 6 - 90).toDouble())
        drawHand(center, minuteAngle, radius * 0.68f, 4f, Color(0xFF1C1C1E))

        // 秒针（蓝色）
        val secondAngle = Math.toRadians((second * 6 - 90).toDouble())
        drawHand(center, secondAngle, radius * 0.76f, 2f, Color(0xFF007AFF))

        // 中心圆点
        drawCircle(color = Color(0xFF007AFF), radius = 6f, center = center)
    }
}

private fun DrawScope.drawHand(
    center: Offset,
    angle: Double,
    length: Float,
    strokeWidth: Float,
    color: Color
) {
    drawLine(
        color = color,
        start = center,
        end = Offset(
            (center.x + length * cos(angle)).toFloat(),
            (center.y + length * sin(angle)).toFloat()
        ),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

@Composable
fun PlaceholderTab(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = Color(0xFFAEAEB2),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "$name 功能开发中",
                fontSize = 16.sp,
                color = Color(0xFF8E8E93)
            )
        }
    }
}
