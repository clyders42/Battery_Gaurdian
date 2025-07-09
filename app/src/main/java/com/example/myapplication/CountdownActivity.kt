package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class CountdownActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null

    private val powerConnectionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status: Int = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            if (isCharging) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set window flags for overlay
        window.apply {
            setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            } else {
                setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
            }
            // Make background transparent
            setBackgroundDrawableResource(android.R.color.transparent)
            setFormat(PixelFormat.TRANSLUCENT)
        }

        val timerDurationSeconds = intent.getLongExtra("TIMER_DURATION_SECONDS", 120L)
        val soundAlert = intent.getBooleanExtra("SOUND_ALERT", false)
        val visualStyle = intent.getFloatExtra("VISUAL_STYLE", 0f)

        if (soundAlert) {
            mediaPlayer = MediaPlayer.create(this, R.raw.beep) // Assuming R.raw.beep exists
            mediaPlayer?.start()
        }

        val intentFilter = IntentFilter(Intent.ACTION_POWER_CONNECTED)
        registerReceiver(powerConnectionReceiver, intentFilter)

        setContent {
            var timeLeft by remember { mutableStateOf(timerDurationSeconds) }

            LaunchedEffect(key1 = timeLeft) {
                if (timeLeft > 0) {
                    delay(1000L)
                    timeLeft--
                }
            }
            CountdownOverlay(timeLeft = timeLeft, visualStyle = visualStyle) {
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        unregisterReceiver(powerConnectionReceiver)
    }
}

@Composable
fun CountdownOverlay(timeLeft: Long, visualStyle: Float, onDismiss: () -> Unit) {
    val minutes = TimeUnit.SECONDS.toMinutes(timeLeft)
    val seconds = timeLeft - TimeUnit.MINUTES.toSeconds(minutes)
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val backgroundColor: Color
    val textColor: Color

    when (visualStyle.toInt()) {
        1 -> {
            backgroundColor = Color.Red.copy(alpha = 0.8f)
            textColor = Color.Yellow
        }
        2 -> {
            backgroundColor = Color.Blue.copy(alpha = 0.8f)
            textColor = Color.Green
        }
        else -> {
            backgroundColor = Color.Black.copy(alpha = 0.8f)
            textColor = Color.White
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "PLUG IN NOW!\n$timeFormatted",
                color = textColor,
                fontSize = 48.sp,
                style = TextStyle(lineHeight = 64.sp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}