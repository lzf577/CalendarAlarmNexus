package com.alarm.plus.nexus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarm.plus.nexus.core.service.RingtoneService
import com.alarm.plus.nexus.core.manager.AlarmScheduler
import com.alarm.plus.nexus.data.local.AlarmEntity

class AlarmRingingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val label = intent.getStringExtra("LABEL") ?: "闹钟响了！"

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = label, fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        Button(
                            onClick = {
                                val serviceIntent = Intent(this@AlarmRingingActivity, RingtoneService::class.java)
                                stopService(serviceIntent)
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).height(60.dp)
                        ) {
                            Text("关闭", fontSize = 24.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        OutlinedButton(
                            onClick = {
                                val serviceIntent = Intent(this@AlarmRingingActivity, RingtoneService::class.java)
                                stopService(serviceIntent)
                                if (alarmId != -1) {
                                    val scheduler = AlarmScheduler(this@AlarmRingingActivity)
                                    val snoozeTime = System.currentTimeMillis() + 5 * 60 * 1000
                                    scheduler.schedule(AlarmEntity(id = alarmId, timeInMillis = snoozeTime, label = "再响"))
                                }
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).height(60.dp)
                        ) {
                            Text("再响 (5分钟后)", fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }
}
