package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldEnjoyment
import com.example.ui.theme.PurpleWorship

@Composable
fun BlueprintScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("blueprint_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Architecture,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "  Engineering Blueprint",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = "Architectural principles, background limits & battery optimization",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section 1: Geofence & Dwell Debouncing Math
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "  1. Geofence & Dwell Debounce Algorithm",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To prevent false alarms while driving or walking through transit zones, the app applies Haversine distance clustering:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = """
if (dist(Anchor, Current) <= GeofenceRadius) {
    dwellSeconds = (Now - AnchorTime) / 1000
    if (dwellSeconds >= Threshold && !Fired) {
        Fired = true
        fetchPOIs(Current.lat, Current.lng)
        triggerPushNotification()
    }
} else {
    // Moved outside radius: reset to new anchor
    Anchor = Current
    AnchorTime = Now
    dwellSeconds = 0
    Fired = false
}
                            """.trimIndent(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Section 2: OS Background Execution Comparison
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = EmeraldEnjoyment,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "  2. OS Background Execution Matrix",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    BlueprintComparisonRow(
                        platform = "Android Foreground Service",
                        badge = "Native / React Native Headless",
                        details = "Type 'location' with ongoing notification. Bypasses Doze mode restrictions. Updates delivered via FusedLocationProviderClient with balanced power interval."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    BlueprintComparisonRow(
                        platform = "iOS CoreLocation",
                        badge = "UIBackgroundModes: location",
                        details = "Uses 'allowsBackgroundLocationUpdates = true' or 'CLVisit' monitoring. In Expo, registered via TaskManager.defineTask(LOCATION_TASK_NAME)."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    BlueprintComparisonRow(
                        platform = "Expo TaskManager",
                        badge = "expo-location geofencing",
                        details = "Location.startGeofencingAsync() or Location.startLocationUpdatesAsync() with deferred updates to conserve power."
                    )
                }
            }
        }

        // Section 3: POI Categorization Strategy
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = PurpleWorship,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "  3. Structured Category Taxonomy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TaxonomyItem("🎉 Enjoyment", "Parks, entertainment centers, live jazz, nightclubs, rooftop lounges (OSM leisure=park, amenity=nightclub).")
                    TaxonomyItem("⛪ Worship", "Churches, cathedrals, temples, chapels, mosques (OSM amenity=place_of_worship).")
                    TaxonomyItem("🍽️ Dining", "Cafes, authentic bistros, street food, fine dining (OSM amenity=restaurant|cafe).")
                    TaxonomyItem("🏛️ Landmarks", "Monuments, ruins, museums, boutique shopping districts (OSM tourism=attraction, historic=*).")
                }
            }
        }

        // Section 4: Battery Optimization Strategies
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "  4. Power Efficiency & Battery Rules",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Batched Location Queries: Do not query GPS on every meter. Enforce minDistance = 25m.\n" +
                               "• Geofence Hardware Offloading: Where supported, register hardware-assisted geofences with Android Location Services or iOS Region Monitoring instead of continuous polling.\n" +
                               "• Debounce Dwell Trigger: Cache POI results per anchor to prevent repeated network calls when sitting at the same cafe.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BlueprintComparisonRow(
    platform: String,
    badge: String,
    details: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = platform,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CyanAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun TaxonomyItem(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}
