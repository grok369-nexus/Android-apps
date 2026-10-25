package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.TransferWithinAStation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DwellViewModel
import com.example.ui.theme.AmberDining
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldEnjoyment

@Composable
fun DeveloperMockLocationPanel(
    viewModel: DwellViewModel,
    modifier: Modifier = Modifier
) {
    val engineState by viewModel.engineState.collectAsState()
    val isExpanded by viewModel.isMockPanelExpanded.collectAsState()
    val latInput by viewModel.customLatitudeInput.collectAsState()
    val lngInput by viewModel.customLongitudeInput.collectAsState()
    val labelInput by viewModel.customLocationLabel.collectAsState()
    val errorMsg by viewModel.mockErrorMessage.collectAsState()

    var isDropdownOpen by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dev_mock_location_panel"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = if (engineState.isMockMode) AmberDining else Color(0xFF334155)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Bar: Developer Tool Badge + Expand/Collapse Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleMockPanelExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = (if (engineState.isMockMode) AmberDining else CyanAccent).copy(alpha = 0.2f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = if (engineState.isMockMode) AmberDining else CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Developer Mock Location Panel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = if (engineState.isMockMode) "MOCK GPS OVERRIDE ACTIVE" else "HARDWARE GPS PASS-THROUGH",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (engineState.isMockMode) AmberDining else EmeraldEnjoyment,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.toggleMockPanelExpanded() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color.LightGray
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // Mock Mode Switch & Override Description
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Override Real GPS Tracking",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Unbinds LocationServices; injects mock coordinates into ViewModel and Foreground Service.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = engineState.isMockMode,
                            onCheckedChange = { viewModel.setMockMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AmberDining,
                                checkedTrackColor = AmberDining.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("switch_mock_gps_override")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preset Location Dropdown Selector
                    Text(
                        text = "1. Select Preset Location or Type Coordinates",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { isDropdownOpen = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_preset_dropdown"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = labelInput.ifBlank { "Select Test City Preset..." },
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.LightGray
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isDropdownOpen,
                            onDismissRequest = { isDropdownOpen = false },
                            modifier = Modifier
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                        ) {
                            viewModel.locationPresets.forEach { preset ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "${preset.name} (${preset.country})",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Lat: ${preset.latitude}, Lng: ${preset.longitude} • ${preset.description}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectPresetCity(preset)
                                        isDropdownOpen = false
                                    },
                                    modifier = Modifier.testTag("dropdown_item_${preset.name}")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Location Name Field
                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { viewModel.setCustomLocationLabel(it) },
                        label = { Text("Location Name / Label", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("e.g. Kyoto Old Town, Shibuya Crossing", color = Color(0xFF64748B)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_mock_label"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = Color(0xFF334155),
                            cursorColor = CyanAccent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Latitude & Longitude Input Fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = latInput,
                            onValueChange = { viewModel.setCustomLatitude(it) },
                            label = { Text("Latitude", color = Color(0xFF94A3B8)) },
                            placeholder = { Text("48.8584", color = Color(0xFF64748B)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_mock_latitude"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = Color(0xFF334155),
                                cursorColor = CyanAccent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = lngInput,
                            onValueChange = { viewModel.setCustomLongitude(it) },
                            label = { Text("Longitude", color = Color(0xFF94A3B8)) },
                            placeholder = { Text("2.2945", color = Color(0xFF64748B)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_mock_longitude"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = Color(0xFF334155),
                                cursorColor = CyanAccent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMsg ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Main Inject Coordinates Button
                    Button(
                        onClick = { viewModel.applyCustomMockLocation() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_inject_mock_location"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PinDrop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Inject & Move Location",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dwell & Geofence Debounce Simulation Controls
                    Text(
                        text = "2. Test Dwell Timer & Geofence Debounce",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Nudge +12m (Stay within geofence radius)
                        OutlinedButton(
                            onClick = { viewModel.nudgeWithinStationaryRadius() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_nudge_within_radius"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldEnjoyment.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldEnjoyment)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.DirectionsWalk, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Nudge ~12m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("(In Radius)", fontSize = 9.sp, color = Color(0xFF94A3B8))
                            }
                        }

                        // Jump +200m (Exceed geofence radius, resetting dwell timer)
                        OutlinedButton(
                            onClick = { viewModel.jumpOutsideGeofence() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_jump_break_geofence"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF43F5E))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.TransferWithinAStation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Jump ~220m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("(Reset Timer)", fontSize = 9.sp, color = Color(0xFF94A3B8))
                            }
                        }

                        // Instant Trigger (Fast forward timer to threshold)
                        OutlinedButton(
                            onClick = { viewModel.triggerDwellImmediately() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_instant_dwell_trigger"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberDining.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberDining)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Fast Dwell", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("(Trigger Now)", fontSize = 9.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live telemetry snippet
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF020617),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Engine: ${if (engineState.isTracking) "RUNNING" else "PAUSED"} • Dist from Anchor: ${engineState.distanceToAnchorMeters}m (Radius: ${engineState.geofenceRadiusMeters}m) • Dwell: ${engineState.currentDwellSeconds}s / ${engineState.thresholdSeconds}s",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
