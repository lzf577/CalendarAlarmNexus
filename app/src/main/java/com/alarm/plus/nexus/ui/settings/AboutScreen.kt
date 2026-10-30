package com.alarm.plus.nexus.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.plus.nexus.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val backgroundColor = Color(0xFFF2F2F7)
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("关于", fontSize = 20.sp, color = Color(0xFF1C1C1E), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color(0xFF1C1C1E))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))
            
            // App Icon
            Surface(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(20.dp)),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "App Icon",
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Name
            Text(
                text = stringResource(id = R.string.app_name),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Version
            Text(
                text = "版本: 1.0.0",
                fontSize = 14.sp,
                color = Color(0xFF8E8E93)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Description Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Text(
                    text = "本应用提供闹钟、世界时钟、秒表和计时器功能，可满足日常生活各场景的使用需求。",
                    fontSize = 14.sp,
                    color = Color(0xFF3A3A3C),
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Guide Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Warning Info",
                            tint = Color(0xFFFF9500),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "后台响铃与权限说明",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = "因部分手机系统深度后台管理限制，若应用后台被系统自动清理，可能会导致闹钟无法准时响铃。为了保证响铃正常，请按以下步骤操作：",
                        fontSize = 13.sp,
                        color = Color(0xFF3A3A3C),
                        lineHeight = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "1. 打开手机的「多任务管理/最近任务」页面；\n2. 找到「日历闹钟Nexus」应用，将其下拉或点击锁定按钮进行「锁定应用」；\n3. 锁定完成后即可随意退出其他界面，闹钟在后台也将能正常精准响起。",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF007AFF),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Copyright
            Text(
                text = "Copyright © 2026 日历闹钟Nexus. All rights reserved.",
                fontSize = 12.sp,
                color = Color(0xFFC7C7CC),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}
