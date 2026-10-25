package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PoiCategory
import com.example.ui.AppScreen
import com.example.ui.DwellViewModel
import com.example.ui.components.DeveloperMockLocationPanel
import com.example.ui.theme.AmberDining
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldEnjoyment
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DwellViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.engineState.collectAsState()
    val threshold by viewModel.settingsRepo.dwellThreshold.collectAsState()
    val presets = viewModel.locationPresets

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header & Status Banner
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
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (state.isTracking) EmeraldEnjoyment else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (state.isTracking) "TRACKING ACTIVE" else "TRACKING PAUSED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isTracking) EmeraldEnjoyment else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = (if (state.isMockMode) AmberDining else CyanAccent).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (state.isMockMode) "MOCK GPS OVERRIDE" else "REAL GPS ACTIVE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.isMockMode) AmberDining else CyanAccent,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = state.locationName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = String.format(
                            Locale.US,
                            "GPS: %.4f, %.4f • Geofence Radius: %dm • Threshold: %ds",
                            state.latitude,
                            state.longitude,
                            state.geofenceRadiusMeters,
                            threshold
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live GPS & Uganda Quick Location Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.refreshLiveGps(context) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_refresh_live_gps"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Live GPS Fix", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.jumpToKawempe() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_jump_to_kawempe"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldEnjoyment),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldEnjoyment)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Place,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kawempe, Uganda", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Status: ${state.gpsStatusMessage}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.isMockMode) AmberDining else EmeraldEnjoyment,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Developer Mock Location Panel (Feature requested by user)
        item {
            DeveloperMockLocationPanel(
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Radar & Dwell Countdown Visualizer
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Stationary Dwell Detection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Debounces minor movement; departures beyond radius reset timer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    DwellRadarWidget(
                        currentSeconds = state.currentDwellSeconds,
                        thresholdSeconds = threshold,
                        isTriggered = state.isDwellTriggered,
                        isTracking = state.isTracking
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.toggleTracking(context) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_toggle_tracking"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (state.isTracking) "Pause Tracking" else "Start Engine")
                        }

                        OutlinedButton(
                            onClick = { viewModel.triggerDwellImmediately() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_trigger_dwell_now"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = CyanAccent
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fast Dwell")
                        }
                    }
                }
            }
        }

        // Quick Travel Destination Presets
        item {
            Text(
                text = "Quick Test Presets",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(presets) { preset ->
                    val isSelected = state.locationName == preset.name
                    Card(
                        modifier = Modifier
                            .clickable { viewModel.selectPresetCity(preset) }
                            .testTag("preset_${preset.name}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyanAccent) else null
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = preset.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Discovered Categories Quick Hub
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discovered Points of Interest (${state.pois.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "View All →",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.selectScreen(AppScreen.POIS) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 4 structured category preview tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryTile(
                    category = PoiCategory.ENJOYMENT,
                    count = state.pois.count { it.category == PoiCategory.ENJOYMENT },
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.selectCategory(PoiCategory.ENJOYMENT)
                    viewModel.selectScreen(AppScreen.POIS)
                }

                CategoryTile(
                    category = PoiCategory.WORSHIP,
                    count = state.pois.count { it.category == PoiCategory.WORSHIP },
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.selectCategory(PoiCategory.WORSHIP)
                    viewModel.selectScreen(AppScreen.POIS)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryTile(
                    category = PoiCategory.FOOD,
                    count = state.pois.count { it.category == PoiCategory.FOOD },
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.selectCategory(PoiCategory.FOOD)
                    viewModel.selectScreen(AppScreen.POIS)
                }

                CategoryTile(
                    category = PoiCategory.OTHER,
                    count = state.pois.count { it.category == PoiCategory.OTHER },
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.selectCategory(PoiCategory.OTHER)
                    viewModel.selectScreen(AppScreen.POIS)
                }
            }
        }
    }
}

@Composable
fun DwellRadarWidget(
    currentSeconds: Int,
    thresholdSeconds: Int,
    isTriggered: Boolean,
    isTracking: Boolean
) {
    val progress = (currentSeconds.toFloat() / thresholdSeconds.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 16.dp.toPx()

            // Outer decorative radar ring
            drawCircle(
                color = Color.Cyan.copy(alpha = if (isTracking) pulseAlpha * 0.3f else 0.1f),
                radius = radius,
                style = Stroke(width = 2.dp.toPx())
            )

            // Inner circle
            drawCircle(
                color = Color.DarkGray.copy(alpha = 0.3f),
                radius = radius * 0.7f,
                style = Stroke(width = 1.dp.toPx())
            )

            // Background arc
            drawArc(
                color = Color.Gray.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )

            // Active progress arc
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(CyanAccent, EmeraldEnjoyment, CyanAccent)
                ),
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isTriggered) Icons.Default.NotificationsActive else Icons.Default.Navigation,
                contentDescription = null,
                tint = if (isTriggered) EmeraldEnjoyment else CyanAccent,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${currentSeconds}s",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isTriggered) "DWELL COMPLETE" else "of ${thresholdSeconds}s dwell",
                style = MaterialTheme.typography.labelSmall,
                color = if (isTriggered) EmeraldEnjoyment else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CategoryTile(
    category: PoiCategory,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag("cat_tile_${category.name}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = category.accentColor.copy(alpha = 0.2f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val icon = when (category) {
                            PoiCategory.ENJOYMENT -> Icons.Default.Celebration
                            PoiCategory.WORSHIP -> Icons.Default.Church
                            PoiCategory.FOOD -> Icons.Default.Restaurant
                            PoiCategory.OTHER -> Icons.Default.Museum
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = category.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "$count spots",
                    style = MaterialTheme.typography.labelSmall,
                    color = category.accentColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = category.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = category.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
