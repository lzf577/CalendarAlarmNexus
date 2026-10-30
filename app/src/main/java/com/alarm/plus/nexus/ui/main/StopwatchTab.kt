package com.alarm.plus.nexus.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StopwatchTab(viewModel: AlarmViewModel) {
    val isRunning by viewModel.isStopwatchRunning.collectAsState()
    val startTime by viewModel.stopwatchStartTime.collectAsState()
    val elapsedBeforeStart by viewModel.stopwatchElapsedBeforeStart.collectAsState()
    val laps by viewModel.stopwatchLaps.collectAsState()

    var currentElapsedMillis by remember { mutableStateOf(elapsedBeforeStart) }

    LaunchedEffect(isRunning, startTime, elapsedBeforeStart) {
        if (isRunning) {
            while (true) {
                withFrameMillis {
                    currentElapsedMillis = elapsedBeforeStart + (System.currentTimeMillis() - startTime)
                }
            }
        } else {
            currentElapsedMillis = elapsedBeforeStart
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top section: Dial and Time
        Box(
            modifier = Modifier
                .size(240.dp)
                .shadow(16.dp, CircleShape, spotColor = Color(0x33000000))
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            StopwatchDial()
            Text(
                text = formatStopwatchTime(currentElapsedMillis),
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF1C1C1E)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Middle section: Laps List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 32.dp)
        ) {
            // Display laps in reverse order (newest first)
            val reversedLaps = laps.reversed()
            val totalLaps = laps.size

            itemsIndexed(reversedLaps) { index, lapTime ->
                val lapNumber = totalLaps - index
                val previousLapTime = if (lapNumber > 1) laps[lapNumber - 2] else 0L
                val lapDuration = lapTime - previousLapTime

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.US, "%02d", lapNumber),
                        fontSize = 16.sp,
                        color = Color(0xFF8E8E93)
                    )
                    Text(
                        text = formatStopwatchTime(lapTime),
                        fontSize = 20.sp,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        text = "+ ${formatStopwatchTime(lapDuration)}",
                        fontSize = 16.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
                HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
            }
        }

        // Bottom section: Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 64.dp, top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Button: Play / Pause
            FloatingActionButton(
                onClick = {
                    if (isRunning) {
                        viewModel.pauseStopwatch()
                    } else {
                        viewModel.startStopwatch()
                    }
                },
                shape = CircleShape,
                containerColor = Color(0xFF007AFF),
                contentColor = Color.White,
                modifier = Modifier.size(72.dp)
            ) {
                if (isRunning) {
                    // Pause Icon
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

            // Right Button: Lap / Reset
            val hasStarted = currentElapsedMillis > 0
            val rightButtonColor = if (hasStarted) Color(0xFF007AFF).copy(alpha = 0.2f) else Color(0xFFF2F2F7)
            val rightIconColor = if (hasStarted) Color(0xFF007AFF) else Color(0xFFAEAEB2)

            FloatingActionButton(
                onClick = {
                    if (isRunning) {
                        viewModel.addStopwatchLap()
                    } else if (hasStarted) {
                        viewModel.resetStopwatch()
                    }
                },
                shape = CircleShape,
                containerColor = rightButtonColor,
                contentColor = rightIconColor,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                modifier = Modifier.size(72.dp)
            ) {
                if (isRunning) {
                    // Flag / Lap icon
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "计次",
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重置",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StopwatchDial(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f

        // Draw faint top ticks
        for (i in -2..2) {
            val angle = Math.toRadians((i * 30 - 90).toDouble())
            val startDist = radius * 0.85f
            val endDist = radius * 0.95f
            
            drawLine(
                color = Color(0xFF333333),
                start = Offset(
                    (center.x + startDist * cos(angle)).toFloat(),
                    (center.y + startDist * sin(angle)).toFloat()
                ),
                end = Offset(
                    (center.x + endDist * cos(angle)).toFloat(),
                    (center.y + endDist * sin(angle)).toFloat()
                ),
                strokeWidth = 4f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

fun formatStopwatchTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hundredths = (millis % 1000) / 10
    return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, hundredths)
}
