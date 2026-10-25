package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "visited_locations")
data class VisitedLocationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val dwellSeconds: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val poiCount: Int,
    val topPoiSummary: String
)

@Entity(tableName = "favorite_pois")
data class FavoritePoiEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String,
    val subType: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int,
    val rating: Double,
    val address: String,
    val summary: String,
    val discoveredAtLocation: String,
    val savedTimestamp: Long = System.currentTimeMillis()
)
