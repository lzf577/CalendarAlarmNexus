package com.alarm.plus.nexus.ui.main

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorldClockTab(
    cities: List<WorldCity>,
    isSelectionMode: Boolean,
    selectedCityIds: Set<Int>,
    onEnterSelectionMode: (Int) -> Unit,
    onToggleSelection: (Int) -> Unit
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTime = System.currentTimeMillis()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            WorldClockHeader(currentTime)
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        items(cities, key = { it.id }) { city ->
            WorldCityItem(
                city = city, 
                currentTime = currentTime,
                isSelectionMode = isSelectionMode,
                isSelected = selectedCityIds.contains(city.id),
                onLongClick = { onEnterSelectionMode(city.id) },
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection(city.id)
                    }
                }
            )
            HorizontalDivider(
                color = Color(0xFFE5E5EA),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun WorldClockHeader(currentTime: Long) {
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormatter = SimpleDateFormat("M月d日 EEE", Locale.CHINESE)
    val date = Date(currentTime)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Globe background placeholder
        Box(
            modifier = Modifier
                .size(240.dp)
                .shadow(16.dp, CircleShape, spotColor = Color(0x33000000))
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            // Circular background for now
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("中国标准时间", fontSize = 14.sp, color = Color(0xFF8E8E93))
                Text(
                    text = timeFormatter.format(date),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                    color = Color(0xFF1C1C1E),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = dateFormatter.format(date),
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorldCityItem(
    city: WorldCity, 
    currentTime: Long,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val tz = TimeZone.getTimeZone(city.timeZoneId)
    val localTz = TimeZone.getDefault()
    
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    timeFormatter.timeZone = tz
    
    val dateFormatter = SimpleDateFormat("M/d EEE", Locale.CHINESE)
    dateFormatter.timeZone = tz
    
    val date = Date(currentTime)
    val timeStr = timeFormatter.format(date)
    
    val offsetMillis = tz.getOffset(currentTime) - localTz.getOffset(currentTime)
    val offsetHours = offsetMillis / (1000 * 60 * 60)
    val offsetMins = (Math.abs(offsetMillis) / (1000 * 60)) % 60
    
    val offsetStr = when {
        offsetMillis < 0 -> if (offsetMins == 0) "晚${-offsetHours}小时" else "晚${-offsetHours}小时${offsetMins}分"
        offsetMillis > 0 -> if (offsetMins == 0) "早${offsetHours}小时" else "早${offsetHours}小时${offsetMins}分"
        else -> "时间相同"
    }
    
    val calendar = Calendar.getInstance(tz)
    calendar.timeInMillis = currentTime
    val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
    val isNight = hourOfDay < 6 || hourOfDay >= 18
    val iconStr = if (isNight) "🌙" else "☀️"
    
    val dateStr = "${dateFormatter.format(date)}, $offsetStr"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelectionMode) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "拖动",
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
            Column {
                Text(city.name, fontSize = 20.sp, color = Color(0xFF1C1C1E))
                Spacer(modifier = Modifier.height(4.dp))
                Text(dateStr, fontSize = 12.sp, color = Color(0xFF8E8E93))
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Day/Night icon
            Text(iconStr, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
            Text(
                text = timeStr,
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF1C1C1E),
                modifier = Modifier.padding(end = if (isSelectionMode) 16.dp else 0.dp)
            )
            
            if (isSelectionMode) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "已选择",
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        drawCircle(
                            color = Color(0xFFD1D1D6),
                            radius = size.minDimension / 2,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                        )
                    }
                }
            }
        }
    }
}
