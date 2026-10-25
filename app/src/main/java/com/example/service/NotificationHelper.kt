package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.PoiCategory
import com.example.data.model.PointOfInterest

object NotificationHelper {

    const val CHANNEL_ID = "dwell_spot_discovery_channel"
    const val CHANNEL_NAME = "Dwell Arrival & POI Discovery"
    const val FOREGROUND_CHANNEL_ID = "dwell_spot_tracking_channel"
    const val FOREGROUND_CHANNEL_NAME = "Dwell Tracking Service"

    const val NOTIFICATION_ID_DWELL = 1001
    const val NOTIFICATION_ID_FOREGROUND = 1002

    const val EXTRA_ACTION = "extra_action"
    const val ACTION_VIEW_POIS = "action_view_pois"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel for Dwell Alert
            val alertChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when dwell time in a new location is satisfied and POIs are found"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(alertChannel)

            // Channel for persistent foreground tracking
            val fgChannel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                FOREGROUND_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background location monitor detecting stationary dwell states"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(fgChannel)
        }
    }

    fun showDwellDiscoveryNotification(
        context: Context,
        locationName: String,
        dwellSeconds: Int,
        pois: List<PointOfInterest>
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open POI screen in app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ACTION, ACTION_VIEW_POIS)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Explore POIs
        val exploreAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_search,
            "Explore Top Places",
            openAppPendingIntent
        ).build()

        // Action: Directions to top place
        val topPlace = pois.firstOrNull()
        val directionsIntent = if (topPlace != null) {
            val uri = Uri.parse("geo:${topPlace.latitude},${topPlace.longitude}?q=${Uri.encode(topPlace.name)}")
            Intent(Intent.ACTION_VIEW, uri)
        } else {
            Intent(context, MainActivity::class.java)
        }
        val directionsPendingIntent = PendingIntent.getActivity(
            context,
            2,
            directionsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val directionsAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_dialog_map,
            "Directions",
            directionsPendingIntent
        ).build()

        // Build summary per category
        val enjoyment = pois.firstOrNull { it.category == PoiCategory.ENJOYMENT }?.name ?: "Scenic Promenade"
        val worship = pois.firstOrNull { it.category == PoiCategory.WORSHIP }?.name ?: "Historic Sanctuary"
        val dining = pois.firstOrNull { it.category == PoiCategory.FOOD }?.name ?: "Artisan Cafe & Bakery"
        val landmarks = pois.firstOrNull { it.category == PoiCategory.OTHER }?.name ?: "Civic Plaza"

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("📍 Welcome to $locationName!")
            .setSummaryText("Dwelled ${dwellSeconds}s • ${pois.size} POIs Found")
            .addLine("🎉 Enjoyment: $enjoyment")
            .addLine("⛪ Worship: $worship")
            .addLine("🍽️ Dining: $dining")
            .addLine("🏛️ Landmark: $landmarks")

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📍 Arrived: $locationName")
            .setContentText("Dwelled for ${dwellSeconds}s. Found ${pois.size} top spots nearby!")
            .setStyle(inboxStyle)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(exploreAction)
            .addAction(directionsAction)

        notificationManager.notify(NOTIFICATION_ID_DWELL, builder.build())
    }
}
