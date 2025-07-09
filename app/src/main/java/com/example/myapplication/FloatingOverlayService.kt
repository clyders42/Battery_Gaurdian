package com.example.myapplication

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class FloatingOverlayService : LifecycleService(), SavedStateRegistryOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry


    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var timerDurationSeconds: Long = 10L
    private var soundAlert: Boolean = false
    private var visualStyle: Float = 0f

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "overlay_service_channel"
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onCreate() {
        savedStateRegistryController.performRestore(null)
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        timerDurationSeconds = intent?.getLongExtra("TIMER_DURATION_SECONDS", 10L) ?: 10L
        soundAlert = intent?.getBooleanExtra("SOUND_ALERT", false) ?: false
        visualStyle = intent?.getFloatExtra("VISUAL_STYLE", 0f) ?: 0f

        showOverlay()
        
        return START_STICKY
    }

    private fun showOverlay() {
        val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
        }

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        composeView = ComposeView(this).apply {
            setContent {
                FloatingCountdownOverlay(
                    initialTime = timerDurationSeconds,
                    visualStyle = visualStyle.toInt(),
                    onDismiss = { stopSelf() }
                )
            }
        }
        composeView!!.setViewTreeLifecycleOwner(this)
        composeView!!.setViewTreeSavedStateRegistryOwner(this)

        windowManager?.addView(composeView, params)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Battery Alert Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Battery Alert Active")
                .setContentText("Monitoring battery level")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Battery Alert Active")
                .setContentText("Monitoring battery level")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (composeView != null) {
            windowManager?.removeView(composeView)
            composeView = null
        }
    }
}

@Composable
fun FloatingCountdownOverlay(
    initialTime: Long = 10L,
    visualStyle: Int = 0,
    onDismiss: () -> Unit
) {
    var timeLeft by remember { mutableStateOf(initialTime) }

    LaunchedEffect(key1 = timeLeft) {
        if (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
    }

    val minutes = TimeUnit.SECONDS.toMinutes(timeLeft)
    val seconds = timeLeft - TimeUnit.MINUTES.toSeconds(minutes)
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val backgroundColor: Color
    val textColor: Color

    when (visualStyle) {
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