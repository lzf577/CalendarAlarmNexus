package com.alarm.plus.nexus.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCityScreen(
    onBack: () -> Unit,
    onCitySelected: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTime = System.currentTimeMillis()
        }
    }

    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("查找城市", color = Color(0xFF8E8E93), fontSize = 16.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索", tint = Color(0xFF8E8E93)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF2F2F7),
                            unfocusedContainerColor = Color(0xFFF2F2F7),
                            disabledContainerColor = Color(0xFFF2F2F7),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Sort */ }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "排序")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val filteredCities = allWorldCities.filter { it.name.contains(searchQuery, ignoreCase = true) }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                item {
                    Text(
                        text = "A",
                        fontSize = 16.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                items(filteredCities) { city ->
                    val tz = TimeZone.getTimeZone(city.timeZoneId)
                    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                    formatter.timeZone = tz
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCitySelected(city.id) }
                            .padding(vertical = 16.dp)
                    ) {
                        Text(city.name, fontSize = 18.sp, color = Color(0xFF1C1C1E))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(formatter.format(Date(currentTime)), fontSize = 14.sp, color = Color(0xFF8E8E93))
                    }
                }
            }
            
            // Side alphabet index mock
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ('A'..'Z').forEach { char ->
                    Text(
                        text = char.toString(),
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
