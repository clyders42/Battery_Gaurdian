package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.os.PowerManager
import com.example.myapplication.BatteryWatcherService
import com.example.myapplication.CountdownActivity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val PREFS_NAME = "BatteryGuardianPrefs"
    private val KEY_TIMER_DURATION = "timer_duration"
    private val KEY_SOUND_ALERT = "sound_alert"
    private val KEY_VISUAL_STYLE = "visual_style"
    private val KEY_SERVICE_ENABLED = "service_enabled"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                var timerDuration by remember { mutableFloatStateOf(prefs.getFloat(KEY_TIMER_DURATION, 2f)) }
                var soundAlert by remember { mutableStateOf(prefs.getBoolean(KEY_SOUND_ALERT, false)) }
                var visualStyle by remember { mutableFloatStateOf(prefs.getFloat(KEY_VISUAL_STYLE, 0f)) }
                var serviceEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_SERVICE_ENABLED, false)) }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SettingsScreen(
                        timerDuration = timerDuration,
                        onTimerDurationChange = {
                            timerDuration = it
                            prefs.edit().putFloat(KEY_TIMER_DURATION, it).apply()
                        },
                        soundAlert = soundAlert,
                        onSoundAlertChange = {
                            soundAlert = it
                            prefs.edit().putBoolean(KEY_SOUND_ALERT, it).apply()
                        },
                        visualStyle = visualStyle,
                        onVisualStyleChange = {
                            visualStyle = it
                            prefs.edit().putFloat(KEY_VISUAL_STYLE, it).apply()
                        },
                        serviceEnabled = serviceEnabled,
                        onStartService = {
                            startBatteryWatcherService()
                            serviceEnabled = true
                            prefs.edit().putBoolean(KEY_SERVICE_ENABLED, true).apply()
                        },
                        onStopService = {
                            stopBatteryWatcherService()
                            serviceEnabled = false
                            prefs.edit().putBoolean(KEY_SERVICE_ENABLED, false).apply()
                        }
                    ,
                        onTestTimer = { startTestTimer() }
                    )
                }
            }
        }
        requestOverlayPermission()
        requestIgnoreBatteryOptimizations()
    }

    private fun startBatteryWatcherService() {
        val intent = Intent(this, BatteryWatcherService::class.java)
        startService(intent)
    }

    private fun stopBatteryWatcherService() {
        val intent = Intent(this, BatteryWatcherService::class.java)
        stopService(intent)
    }

    private fun startTestTimer() {
        val overlayIntent = Intent(this, CountdownActivity::class.java)
        overlayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        overlayIntent.putExtra("TIMER_DURATION_SECONDS", 10L) // 10 seconds for testing
        overlayIntent.putExtra("SOUND_ALERT", true)
        overlayIntent.putExtra("VISUAL_STYLE", 0f)
        startActivity(overlayIntent)
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = packageName
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent()
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.setData(Uri.parse("package:" + packageName))
                startActivity(intent)
            }
        }
    }
}

@Composable
fun SettingsScreen(
    timerDuration: Float,
    onTimerDurationChange: (Float) -> Unit,
    soundAlert: Boolean,
    onSoundAlertChange: (Boolean) -> Unit,
    visualStyle: Float,
    onVisualStyleChange: (Float) -> Unit,
    serviceEnabled: Boolean,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onTestTimer: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = if (serviceEnabled) "Enabled" else "Disabled",
            modifier = Modifier.padding(bottom = 16.dp))

        Text(text = "Timer Duration: ${timerDuration.toInt()} minutes")
        Slider(
            value = timerDuration,
            onValueChange = onTimerDurationChange,
            valueRange = 1f..5f,
            steps = 3,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        Text(text = "Visual Style: ${visualStyle.toInt()}")
        Slider(
            value = visualStyle,
            onValueChange = onVisualStyleChange,
            valueRange = 0f..2f, // Assuming 3 styles (0, 1, 2)
            steps = 2,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Sound Alert")
            Switch(
                checked = soundAlert,
                onCheckedChange = onSoundAlertChange
            )
        }

        Button(onClick = onStartService) {
            Text("Enable Battery Guardian")
        }
        Button(onClick = onStopService) {
            Text("Disable Battery Guardian")
        }
        Button(onClick = onTestTimer) {
            Text("Test Timer")
        }
    }
}
