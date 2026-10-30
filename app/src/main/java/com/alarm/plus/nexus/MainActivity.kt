package com.alarm.plus.nexus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        if (!android.provider.Settings.canDrawOverlays(this)) {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            try {
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = android.net.Uri.parse("package:$packageName")
            try {
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: com.alarm.plus.nexus.ui.main.AlarmViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf("main") }
    var editAlarmId by remember { mutableStateOf<Int?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("world_clock_prefs", android.content.Context.MODE_PRIVATE) }
    
    var cities by remember { 
        val savedCityIds = prefs.getString("saved_cities", "") ?: ""
        val initialList = if (savedCityIds.isEmpty()) {
            emptyList()
        } else {
            val ids = savedCityIds.split(",").mapNotNull { it.toIntOrNull() }
            ids.mapNotNull { id -> com.alarm.plus.nexus.ui.main.allWorldCities.find { it.id == id } }
        }
        mutableStateOf(initialList)
    }
    
    LaunchedEffect(cities) {
        val idsStr = cities.joinToString(",") { it.id.toString() }
        prefs.edit().putString("saved_cities", idsStr).apply()
    }
    
    var mainSelectedTab by remember { mutableStateOf(0) }

    androidx.activity.compose.BackHandler(enabled = currentScreen != "main") {
        if (currentScreen == "about") {
            currentScreen = "settings"
        } else {
            currentScreen = "main"
            editAlarmId = null
        }
    }

    when (currentScreen) {
        "main" -> com.alarm.plus.nexus.ui.main.MainScreen(
            viewModel = viewModel,
            cities = cities,
            onCitiesChanged = { cities = it },
            onNavigateToAddAlarm = { 
                editAlarmId = null
                currentScreen = "add_alarm" 
            },
            onNavigateToEditAlarm = { id ->
                editAlarmId = id
                currentScreen = "add_alarm"
            },
            onNavigateToSettings = {
                currentScreen = "settings"
            },
            onNavigateToAddCity = {
                currentScreen = "add_city"
            },
            selectedTab = mainSelectedTab,
            onTabSelected = { mainSelectedTab = it }
        )
        "add_alarm" -> com.alarm.plus.nexus.ui.main.AddAlarmScreen(
            viewModel = viewModel,
            alarmId = editAlarmId,
            onBack = { 
                currentScreen = "main"
                editAlarmId = null
            }
        )
        "settings" -> com.alarm.plus.nexus.ui.settings.SettingsScreen(
            onBack = { currentScreen = "main" },
            onNavigateToAbout = { currentScreen = "about" }
        )
        "about" -> com.alarm.plus.nexus.ui.settings.AboutScreen(
            onBack = { currentScreen = "settings" }
        )
        "add_city" -> com.alarm.plus.nexus.ui.main.AddCityScreen(
            onBack = { currentScreen = "main" },
            onCitySelected = { cityId ->
                val cityToAdd = com.alarm.plus.nexus.ui.main.allWorldCities.find { it.id == cityId }
                if (cityToAdd != null && !cities.any { it.id == cityId }) {
                    cities = cities + cityToAdd
                }
                mainSelectedTab = 1
                currentScreen = "main"
            }
        )
    }
}
