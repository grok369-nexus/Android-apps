package com.example.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class DwellTrackingService : Service() {

    private lateinit var dwellManager: LocationDwellManager

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        dwellManager = LocationDwellManager.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            dwellManager.stopTracking()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, DwellTrackingService::class.java).apply {
            this.action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationHelper.FOREGROUND_CHANNEL_ID)
            .setContentTitle("DwellSpot Active")
            .setContentText("Monitoring dwell time & nearby points of interest...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Pause Tracking", stopPendingIntent)
            .build()

        startForeground(NotificationHelper.NOTIFICATION_ID_FOREGROUND, notification)
        dwellManager.startTracking()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        dwellManager.stopTracking()
    }

    companion object {
        const val ACTION_START_SERVICE = "action_start_dwell_service"
        const val ACTION_STOP_SERVICE = "action_stop_dwell_service"

        fun start(context: Context) {
            val intent = Intent(context, DwellTrackingService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, DwellTrackingService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
