package com.example.data.repository

import com.example.data.local.FavoritePoiEntity
import com.example.data.local.TravelHistoryDao
import com.example.data.local.VisitedLocationEntity
import com.example.data.model.PointOfInterest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TravelHistoryRepository(private val dao: TravelHistoryDao) {

    val visitedLocations: Flow<List<VisitedLocationEntity>> = dao.getAllVisitedLocations()

    val favoritePois: Flow<List<FavoritePoiEntity>> = dao.getAllFavoritePois()

    val favoritePoiIds: Flow<Set<String>> = dao.getFavoritePoiIds().map { it.toSet() }

    suspend fun recordVisitedLocation(
        locationName: String,
        latitude: Double,
        longitude: Double,
        dwellSeconds: Int,
        poiCount: Int,
        topPoiSummary: String
    ) {
        val entity = VisitedLocationEntity(
            locationName = locationName,
            latitude = latitude,
            longitude = longitude,
            dwellSeconds = dwellSeconds,
            poiCount = poiCount,
            topPoiSummary = topPoiSummary
        )
        dao.insertVisitedLocation(entity)
    }

    suspend fun deleteVisitedLocation(id: String) {
        dao.deleteVisitedLocation(id)
    }

    suspend fun clearVisitedLocations() {
        dao.clearAllVisitedLocations()
    }

    suspend fun toggleFavorite(poi: PointOfInterest, currentLocationName: String) {
        val isFav = dao.isPoiFavorited(poi.id)
        if (isFav) {
            dao.deleteFavoritePoi(poi.id)
        } else {
            val entity = FavoritePoiEntity(
                id = poi.id,
                name = poi.name,
                category = poi.category.name,
                subType = poi.subType,
                latitude = poi.latitude,
                longitude = poi.longitude,
                distanceMeters = poi.distanceMeters,
                rating = poi.rating,
                address = poi.address,
                summary = poi.summary,
                discoveredAtLocation = currentLocationName
            )
            dao.insertFavoritePoi(entity)
        }
    }

    suspend fun removeFavorite(poiId: String) {
        dao.deleteFavoritePoi(poiId)
    }

    suspend fun clearAllFavorites() {
        dao.clearAllFavoritePois()
    }
}
