package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmberDining
import com.example.ui.theme.EmeraldEnjoyment
import com.example.ui.theme.PurpleWorship
import com.example.ui.theme.RoseLandmarks

enum class PoiCategory(
    val displayName: String,
    val description: String,
    val iconName: String,
    val accentColor: Color
) {
    ENJOYMENT(
        displayName = "Enjoyment & Nightlife",
        description = "Parks, Entertainment, Nightlife & Clubs",
        iconName = "celebration",
        accentColor = EmeraldEnjoyment
    ),
    WORSHIP(
        displayName = "Places of Worship",
        description = "Churches, Temples, Mosques & Sacred Sites",
        iconName = "church",
        accentColor = PurpleWorship
    ),
    FOOD(
        displayName = "Restaurants & Eateries",
        description = "Cafes, Local Eateries, Bistros & Bars",
        iconName = "restaurant",
        accentColor = AmberDining
    ),
    OTHER(
        displayName = "Landmarks & Shopping",
        description = "Monuments, Museums, Plazas & Shopping",
        iconName = "museum",
        accentColor = RoseLandmarks
    );

    companion object {
        fun fromString(str: String?): PoiCategory {
            return entries.firstOrNull { it.name.equals(str, ignoreCase = true) } ?: OTHER
        }
    }
}

data class PointOfInterest(
    val id: String,
    val name: String,
    val category: PoiCategory,
    val subType: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int,
    val rating: Double,
    val address: String,
    val isOpenNow: Boolean = true,
    val summary: String = ""
)

data class DwellRecord(
    val id: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val dwellSeconds: Int,
    val poiCount: Int,
    val topPoiSummary: String
)

data class LocationPreset(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val description: String
)
