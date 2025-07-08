package com.example.myapplication

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication.CountdownActivity

class BatteryWatcherService : Service() {

    private val CHANNEL_ID = "BatteryWatcherChannel"

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = level * 100 / scale.toFloat()

            Log.d("BatteryWatcherService", "Battery level: $batteryPct%")

            if (batteryPct <= 1.0f) {
                val prefs = getSharedPreferences("BatteryGuardianPrefs", Context.MODE_PRIVATE)
                val timerDuration = prefs.getFloat("timer_duration", 2f).toLong() * 60 // Convert minutes to seconds
                val soundAlert = prefs.getBoolean("sound_alert", false)

                val overlayIntent = Intent(context, CountdownActivity::class.java)
                overlayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                overlayIntent.putExtra("TIMER_DURATION_SECONDS", timerDuration)
                overlayIntent.putExtra("SOUND_ALERT", soundAlert)
                startActivity(overlayIntent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = createNotification()
        startForeground(1, notification)

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Battery Watcher Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Battery Guardian")
            .setContentText("Monitoring battery level...")
            .setSmallIcon(R.mipmap.ic_launcher_round) // Use your app's icon
            .build()
    }
}
