package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FavoritePoiEntity
import com.example.data.local.VisitedLocationEntity
import com.example.data.model.PoiCategory
import com.example.ui.DwellViewModel
import com.example.ui.theme.AmberDining
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldEnjoyment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: DwellViewModel,
    modifier: Modifier = Modifier
) {
    val visitedList by viewModel.visitedLocations.collectAsState()
    val favoritesList by viewModel.favoritePois.collectAsState()
    val context = LocalContext.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("history_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Travel Log & Saved Places",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Persisted locally via Android Room Database",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val hasItems = if (selectedTabIndex == 0) visitedList.isNotEmpty() else favoritesList.isNotEmpty()
            if (hasItems) {
                IconButton(
                    onClick = { showClearConfirmDialog = true },
                    modifier = Modifier.testTag("btn_clear_history")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Room Database Segment Tabs
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = CyanAccent,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = CyanAccent
                )
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Text(
                        text = "Visited Arrivals (${visitedList.size})",
                        fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                    )
                },
                modifier = Modifier.testTag("tab_visited_locations")
            )

            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Text(
                        text = "Favorited POIs (${favoritesList.size})",
                        fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                    )
                },
                modifier = Modifier.testTag("tab_favorite_pois")
            )
        }

        if (selectedTabIndex == 0) {
            // Tab 1: Visited Locations from Room
            if (visitedList.isEmpty()) {
                EmptyStateCard(
                    title = "No Visited Locations Yet",
                    subtitle = "When you stay in a new location for your dwell threshold, arrival records are automatically saved here in your local Room database."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("visited_locations_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visitedList, key = { it.id }) { record ->
                        VisitedLocationCard(
                            record = record,
                            onDelete = { viewModel.deleteVisitedLocation(record.id) }
                        )
                    }
                }
            }
        } else {
            // Tab 2: Favorited POIs from Room
            if (favoritesList.isEmpty()) {
                EmptyStateCard(
                    title = "No Favorited Places Yet",
                    subtitle = "Tap the bookmark icon on any point of interest in the 'Places' tab to save top restaurants, churches, parks, and landmarks to your local Room database."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("favorite_pois_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(favoritesList, key = { it.id }) { fav ->
                        FavoritePoiCard(
                            favorite = fav,
                            onRemove = { viewModel.removeFavoritePoi(fav.id) },
                            onGetDirections = {
                                val uri = Uri.parse("geo:${fav.latitude},${fav.longitude}?q=${Uri.encode(fav.name)}")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }

    // Confirm Clear Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(if (selectedTabIndex == 0) "Clear Visited History?" else "Clear All Favorites?")
            },
            text = {
                Text(
                    if (selectedTabIndex == 0)
                        "This will permanently remove all visited arrival records from the local Room database."
                    else
                        "This will permanently remove all favorited points of interest from the local Room database."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedTabIndex == 0) {
                            viewModel.clearVisitedHistory()
                        } else {
                            viewModel.clearAllFavorites()
                        }
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun VisitedLocationCard(
    record: VisitedLocationEntity,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        .format(Date(record.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = record.locationName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldEnjoyment.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${record.dwellSeconds}s Dwelled",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldEnjoyment,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$dateStr • Lat: ${String.format(Locale.US, "%.4f", record.latitude)}, Lng: ${String.format(Locale.US, "%.4f", record.longitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "${record.poiCount} Points of Interest Found:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = record.topPoiSummary.ifBlank { "Top categorized spots detected" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FavoritePoiCard(
    favorite: FavoritePoiEntity,
    onRemove: () -> Unit,
    onGetDirections: () -> Unit
) {
    val category = PoiCategory.fromString(favorite.category)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = category.accentColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = category.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = category.accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Remove Favorite",
                        tint = CyanAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = favorite.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = favorite.subType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = " • ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = AmberDining,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = String.format(Locale.US, "%.1f", favorite.rating),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (favorite.address.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = favorite.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discovered in ${favorite.discoveredAtLocation}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )

                OutlinedButton(
                    onClick = onGetDirections,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = CyanAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Directions", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
                fontSize = 12.sp
            )
        }
    }
}
