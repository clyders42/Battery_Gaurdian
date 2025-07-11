package com.clyde.nomody

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.clyde.nomody.ui.theme.MyApplicationTheme
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
    private var overlaySize: Int = 0
    private var customSoundUri: String? = null

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
        overlaySize = intent?.getIntExtra("OVERLAY_SIZE", 0) ?: 0
        customSoundUri = intent?.getStringExtra("CUSTOM_SOUND_URI")

        showOverlay()
        
        return START_STICKY
    }

    private fun showOverlay() {
        if (composeView != null && composeView?.parent != null) {
            // Overlay is already shown and attached to window, do nothing
            return
        }

        val screenHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager?.currentWindowMetrics
            windowMetrics?.bounds?.height() ?: 0
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay?.getMetrics(displayMetrics)
            displayMetrics.heightPixels
        }

        val overlayHeight = when (overlaySize) {
            1 -> screenHeight / 2 // Half screen
            2 -> screenHeight / 3 // One-third screen
            else -> WindowManager.LayoutParams.MATCH_PARENT // Full screen
        }

        val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayHeight,
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
                overlayHeight,
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
                MyApplicationTheme(dynamicColor = false) {
                    FloatingCountdownOverlay(
                        initialTime = timerDurationSeconds,
                        overlaySize = overlaySize,
                        soundAlert = soundAlert,
                        customSoundUri = customSoundUri,
                        onDismiss = { stopSelf() }
                    )
                }
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
        Log.d("FloatingOverlayService", "onDestroy called. Removing overlay.")
        if (composeView != null) {
            windowManager?.removeView(composeView)
            composeView = null
        }
    }
}

@Composable
fun FloatingCountdownOverlay(
    initialTime: Long = 10L,
    overlaySize: Int,
    soundAlert: Boolean,
    customSoundUri: String?,
    onDismiss: () -> Unit
) {
    var timeLeft by remember { mutableStateOf(initialTime) }
    val context = LocalContext.current

    if (soundAlert) {
        DisposableEffect(Unit) {
            val mediaPlayer = if (customSoundUri != null) {
                try {
                    MediaPlayer().apply {
                        setDataSource(customSoundUri)
                        prepare()
                    }
                } catch (e: Exception) {
                    Log.e("FloatingOverlayService", "Error setting data source for custom sound", e)
                    // Fallback to default sound
                    MediaPlayer.create(context, R.raw.low_battery_sound)
                }
            } else {
                MediaPlayer.create(context, R.raw.low_battery_sound)
            }
            mediaPlayer?.start()

            onDispose {
                mediaPlayer?.release()
            }
        }
    }

    LaunchedEffect(key1 = timeLeft) {
        if (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
    }

    val minutes = TimeUnit.SECONDS.toMinutes(timeLeft)
    val seconds = timeLeft - TimeUnit.MINUTES.toSeconds(minutes)
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val backgroundColor = Color.Black.copy(alpha = 0.8f)
    val textColor = Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        when (overlaySize) {
            // Full screen
            0 -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.battery_low_alert),
                        contentDescription = "Low Battery Alert"
                    )
                    Spacer(modifier = Modifier.height(75.dp))
                    Text(
                        text = timeFormatted,
                        color = textColor,
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(50.dp))
                    Button(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                }
            }
            // Half screen
            1 -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.battery_low_alert),
                        contentDescription = "Low Battery Alert",
                        modifier = Modifier.height(250.dp) // Smaller image
                    )
                    Spacer(modifier = Modifier.height(25.dp))
                    Text(
                        text = timeFormatted,
                        color = textColor,
                        style = MaterialTheme.typography.headlineLarge // Slightly smaller text
                    )
                    Spacer(modifier = Modifier.height(25.dp))
                    Button(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                }
            }
            // One-third screen
            2 -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center, // Changed for manual spacing
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.battery_low_alert),
                        contentDescription = "Low Battery Alert",
                        modifier = Modifier.height(100.dp) // Slightly larger image
                    )
                    // Spacer added for manual control over padding
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = timeFormatted,
                            color = textColor,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Button(onClick = onDismiss) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}

