package com.clyde.nomody

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.clyde.nomody.ui.theme.MyApplicationTheme
import com.clyde.nomody.ui.theme.PurpleGrey40
import com.clyde.nomody.ui.theme.SliderActiveTrackColor
import com.clyde.nomody.ui.theme.SliderInactiveTrackColor
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private val PREFS_NAME = "BatteryGuardianPrefs"
    private val KEY_TIMER_DURATION = "timer_duration"
    private val KEY_SOUND_ALERT = "sound_alert"
    private val KEY_OVERLAY_SIZE = "overlay_size"
    private val KEY_SERVICE_ENABLED = "service_enabled"
    private val KEY_CUSTOM_SOUND_URI = "custom_sound_uri"
    private val REQUEST_CODE_OVERLAY_PERMISSION = 1001
    private fun copySoundToInternalStorage(context: Context, uri: Uri): String? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val file = File(context.filesDir, "custom_sound.mp3")
                val outputStream = FileOutputStream(file)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
                return file.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme(dynamicColor = false) {
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                var timerDuration by remember { mutableFloatStateOf(prefs.getFloat(KEY_TIMER_DURATION, 2f)) }
                var soundAlert by remember { mutableStateOf(prefs.getBoolean(KEY_SOUND_ALERT, false)) }
                var overlaySize by remember { mutableStateOf(prefs.getInt(KEY_OVERLAY_SIZE, 0)) }
                var serviceEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_SERVICE_ENABLED, false)) }
                var customSoundUri by remember { mutableStateOf(prefs.getString(KEY_CUSTOM_SOUND_URI, null)) }

                val selectSoundLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let {
                        val path = copySoundToInternalStorage(applicationContext, it)
                        if (path != null) {
                            customSoundUri = path
                            prefs.edit().putString(KEY_CUSTOM_SOUND_URI, path).apply()
                        }
                    }
                }

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
                        overlaySize = overlaySize,
                        onOverlaySizeChange = {
                            overlaySize = it
                            prefs.edit().putInt(KEY_OVERLAY_SIZE, it).apply()
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
                        },
                        onTestTimer = { startTestTimer() },
                        onSelectSound = {
                            selectSoundLauncher.launch("audio/mpeg")
                        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        } else {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val overlaySize = prefs.getInt(KEY_OVERLAY_SIZE, 0)
            val customSoundUri = prefs.getString(KEY_CUSTOM_SOUND_URI, null)

            val overlayIntent = Intent(this, FloatingOverlayService::class.java)
            overlayIntent.putExtra("TIMER_DURATION_SECONDS", 10L) // 10 seconds for testing
            overlayIntent.putExtra("SOUND_ALERT", true)
            overlayIntent.putExtra("OVERLAY_SIZE", overlaySize)
            overlayIntent.putExtra("CUSTOM_SOUND_URI", customSoundUri)
            startService(overlayIntent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                // Permission granted, start the service
                startTestTimer() // This will now proceed to start the service
            }
        }
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
    overlaySize: Int,
    onOverlaySizeChange: (Int) -> Unit,
    serviceEnabled: Boolean,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onTestTimer: () -> Unit,
    onSelectSound: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (serviceEnabled) "Enabled" else "Disabled",
            modifier = Modifier.padding(bottom = 300.dp),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Text(text = "Timer Duration: ${String.format("%.1f", timerDuration)} minutes")
        Slider(
            value = timerDuration,
            onValueChange = onTimerDurationChange,
            valueRange = 1f..2f,
            steps = 1,
            colors = SliderDefaults.colors(
                activeTrackColor = SliderActiveTrackColor,
                inactiveTrackColor = SliderInactiveTrackColor
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Overlay Size")
        Spacer(modifier = Modifier.height(8.dp))
        OverlaySizeSelector(
            selectedSize = overlaySize,
            onSizeSelected = onOverlaySizeChange
        )

        Spacer(modifier = Modifier.height(16.dp))

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
            Button(onClick = onSelectSound) {
                Text("Select Sound")
            }
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

@Composable
fun OverlaySizeSelector(
    selectedSize: Int,
    onSizeSelected: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Full screen option
        SizeBox(
            sizeFraction = 1f,
            isSelected = selectedSize == 0,
            onClick = { onSizeSelected(0) }
        )
        Spacer(modifier = Modifier.width(16.dp))
        // Half screen option
        SizeBox(
            sizeFraction = 0.5f,
            isSelected = selectedSize == 1,
            onClick = { onSizeSelected(1) }
        )
        Spacer(modifier = Modifier.width(16.dp))
        // One-third screen option
        SizeBox(
            sizeFraction = 1f / 3f,
            isSelected = selectedSize == 2,
            onClick = { onSizeSelected(2) }
        )
    }
}

@Composable
fun SizeBox(
    sizeFraction: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 60.dp)
            .border(width = 2.dp, color = borderColor)
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            // Draw the grey part
            drawRect(
                color = PurpleGrey40,
                size = Size(canvasWidth, canvasHeight * sizeFraction)
            )
            // Draw the black part
            if (sizeFraction < 1f) {
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(0f, canvasHeight * sizeFraction),
                    size = Size(canvasWidth, canvasHeight * (1 - sizeFraction))
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    MyApplicationTheme {
        SettingsScreen(
            timerDuration = 2f,
            onTimerDurationChange = {},
            soundAlert = true,
            onSoundAlertChange = {},
            overlaySize = 0,
            onOverlaySizeChange = {},
            serviceEnabled = true,
            onStartService = {},
            onStopService = {},
            onTestTimer = {},
            onSelectSound = {}
        )
    }
}
