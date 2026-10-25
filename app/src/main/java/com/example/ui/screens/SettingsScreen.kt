package com.example.ui.screens

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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DwellViewModel
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldEnjoyment

@Composable
fun SettingsScreen(
    viewModel: DwellViewModel,
    modifier: Modifier = Modifier
) {
    val threshold by viewModel.settingsRepo.dwellThreshold.collectAsState()
    val radius by viewModel.settingsRepo.geofenceRadius.collectAsState()
    val batterySaver by viewModel.settingsRepo.batterySaver.collectAsState()
    val notifSound by viewModel.settingsRepo.notificationSound.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Tracking & Dwell Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Customize stationary detection, geofence radius & battery profile",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Developer Mock Location Panel
        item {
            com.example.ui.components.DeveloperMockLocationPanel(
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 1. Dwell Threshold Slider Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "  Dwell Time Threshold",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val mins = threshold / 60
                        val secs = threshold % 60
                        val displayTime = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"

                        Text(
                            text = displayTime,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanAccent
                        )
                    }

                    Text(
                        text = "Time user must remain stationary before triggering POI discovery (30s – 300s).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Slider(
                        value = threshold.toFloat(),
                        onValueChange = { viewModel.setDwellThreshold(it.toInt()) },
                        valueRange = 30f..300f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanAccent,
                            activeTrackColor = CyanAccent
                        ),
                        modifier = Modifier.testTag("slider_dwell_threshold")
                    )

                    // Quick presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(30, 60, 120, 180, 300).forEach { presetVal ->
                            val isSelected = threshold == presetVal
                            val label = when (presetVal) {
                                30 -> "30s"
                                60 -> "1m"
                                120 -> "2m"
                                180 -> "3m"
                                else -> "5m"
                            }
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setDwellThreshold(presetVal) },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.elevatedFilterChipColors(
                                    selectedContainerColor = CyanAccent,
                                    selectedLabelColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.testTag("preset_threshold_$presetVal")
                            )
                        }
                    }
                }
            }
        }

        // 2. Geofence Debounce Radius Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = EmeraldEnjoyment,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "  Stationary Radius",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "${radius} meters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldEnjoyment
                        )
                    }

                    Text(
                        text = "Debounce buffer: movements under ${radius}m are treated as dwelling at the same place. Moving beyond ${radius}m resets the dwell timer for the new area.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Slider(
                        value = radius.toFloat(),
                        onValueChange = { viewModel.setGeofenceRadius(it.toInt()) },
                        valueRange = 30f..250f,
                        steps = 6,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldEnjoyment,
                            activeTrackColor = EmeraldEnjoyment
                        ),
                        modifier = Modifier.testTag("slider_geofence_radius")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(40, 80, 120, 200).forEach { rVal ->
                            val isSelected = radius == rVal
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setGeofenceRadius(rVal) },
                                label = { Text("${rVal}m", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.elevatedFilterChipColors(
                                    selectedContainerColor = EmeraldEnjoyment,
                                    selectedLabelColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Battery Optimization Profile
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.padding(start = 10.dp)) {
                                Text(
                                    text = "Eco-Travel Battery Saver",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (batterySaver) "Enabled: Balanced GPS, 15s intervals" else "High Precision: Continuous 5s GPS",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = batterySaver,
                            onCheckedChange = { viewModel.toggleBatterySaver(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyanAccent,
                                checkedTrackColor = CyanAccent.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("switch_battery_saver")
                        )
                    }
                }
            }
        }

        // 4. Notifications Preference
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.padding(start = 10.dp)) {
                                Text(
                                    text = "Arrival Push Alerts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Rich push notification with directions & categories",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = notifSound,
                            onCheckedChange = { viewModel.settingsRepo.toggleNotificationSound(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyanAccent,
                                checkedTrackColor = CyanAccent.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("switch_notifications")
                        )
                    }
                }
            }
        }
    }
}
