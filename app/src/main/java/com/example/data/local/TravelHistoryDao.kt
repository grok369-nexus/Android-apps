package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelHistoryDao {

    // --- Visited Locations ---
    @Query("SELECT * FROM visited_locations ORDER BY timestamp DESC")
    fun getAllVisitedLocations(): Flow<List<VisitedLocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisitedLocation(location: VisitedLocationEntity)

    @Query("DELETE FROM visited_locations WHERE id = :id")
    suspend fun deleteVisitedLocation(id: String)

    @Query("DELETE FROM visited_locations")
    suspend fun clearAllVisitedLocations()

    // --- Favorite POIs ---
    @Query("SELECT * FROM favorite_pois ORDER BY savedTimestamp DESC")
    fun getAllFavoritePois(): Flow<List<FavoritePoiEntity>>

    @Query("SELECT id FROM favorite_pois")
    fun getFavoritePoiIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoritePoi(poi: FavoritePoiEntity)

    @Query("DELETE FROM favorite_pois WHERE id = :id")
    suspend fun deleteFavoritePoi(id: String)

    @Query("DELETE FROM favorite_pois")
    suspend fun clearAllFavoritePois()

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_pois WHERE id = :id)")
    suspend fun isPoiFavorited(id: String): Boolean
}
