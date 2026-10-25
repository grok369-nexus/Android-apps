package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FavoritePoiEntity
import com.example.data.local.VisitedLocationEntity
import com.example.data.model.LocationPreset
import com.example.data.model.PoiCategory
import com.example.data.model.PointOfInterest
import com.example.service.DwellTrackingService
import com.example.service.LocationDwellManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen(val label: String, val testTag: String) {
    DASHBOARD("Radar", "nav_dashboard"),
    POIS("Places", "nav_pois"),
    HISTORY("History", "nav_history"),
    SETTINGS("Settings", "nav_settings"),
    BLUEPRINT("Blueprint", "nav_blueprint")
}

class DwellViewModel(application: Application) : AndroidViewModel(application) {

    private val dwellManager = LocationDwellManager.getInstance(application)
    val settingsRepo = dwellManager.settingsRepository
    private val travelHistoryRepo = dwellManager.travelHistoryRepository

    val engineState = dwellManager.engineState

    // Room Database Reactive Streams
    val visitedLocations: StateFlow<List<VisitedLocationEntity>> = travelHistoryRepo.visitedLocations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritePois: StateFlow<List<FavoritePoiEntity>> = travelHistoryRepo.favoritePois
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritePoiIds: StateFlow<Set<String>> = travelHistoryRepo.favoritePoiIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedCategory = MutableStateFlow<PoiCategory?>(null)
    val selectedCategory: StateFlow<PoiCategory?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // --- Developer Mock Location Panel States (Collapsed by default so Live GPS is primary) ---
    private val _isMockPanelExpanded = MutableStateFlow(false)
    val isMockPanelExpanded: StateFlow<Boolean> = _isMockPanelExpanded.asStateFlow()

    private val _customLatitudeInput = MutableStateFlow("0.3676")
    val customLatitudeInput: StateFlow<String> = _customLatitudeInput.asStateFlow()

    private val _customLongitudeInput = MutableStateFlow("32.5562")
    val customLongitudeInput: StateFlow<String> = _customLongitudeInput.asStateFlow()

    private val _customLocationLabel = MutableStateFlow("Kawempe • Kampala, Uganda")
    val customLocationLabel: StateFlow<String> = _customLocationLabel.asStateFlow()

    private val _mockErrorMessage = MutableStateFlow<String?>(null)
    val mockErrorMessage: StateFlow<String?> = _mockErrorMessage.asStateFlow()

    val locationPresets = listOf(
        LocationPreset("Kawempe • Kampala", "Uganda", 0.3676, 32.5562, "Kawempe Division, Bombo Road, Kampala"),
        LocationPreset("Kampala Central • Nakasero", "Uganda", 0.3163, 32.5822, "Kampala Central Business District"),
        LocationPreset("Entebbe • Lake Victoria", "Uganda", 0.0512, 32.4637, "Botanical gardens & lakeside beaches"),
        LocationPreset("Nairobi • Westlands", "Kenya", -1.2675, 36.8121, "Urban cafes, shopping & art galleries"),
        LocationPreset("London • Westminster", "UK", 51.5007, -0.1246, "Big Ben, historic pubs & abbey"),
        LocationPreset("Paris • Champ de Mars", "France", 48.8584, 2.2945, "Iconic Eiffel Tower & Seine gardens"),
        LocationPreset("New York • Times Square", "USA", 40.7580, -73.9855, "Broadway theater district & dining"),
        LocationPreset("Tokyo • Shibuya Crossing", "Japan", 35.6595, 139.7005, "Bustling nightlife, izakayas & shrines")
    )

    val filteredPois: StateFlow<List<PointOfInterest>> = combine(
        engineState,
        selectedCategory,
        searchQuery
    ) { state, cat, query ->
        state.pois.filter { poi ->
            val matchesCategory = (cat == null || poi.category == cat)
            val matchesQuery = query.isBlank() ||
                    poi.name.contains(query, ignoreCase = true) ||
                    poi.subType.contains(query, ignoreCase = true) ||
                    poi.address.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectCategory(category: PoiCategory?) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Re-queries hardware FusedLocationProviderClient for live GPS
     */
    fun refreshLiveGps(context: Context) {
        dwellManager.setMockMode(false)
        dwellManager.refreshLiveLocation()
        DwellTrackingService.start(context)
    }

    /**
     * Jump directly to Kawempe, Kampala, Uganda
     */
    fun jumpToKawempe() {
        dwellManager.jumpToKawempeUganda()
    }

    fun toggleTracking(context: Context) {
        val isCurrentlyTracking = engineState.value.isTracking
        if (isCurrentlyTracking) {
            DwellTrackingService.stop(context)
            dwellManager.stopTracking()
        } else {
            DwellTrackingService.start(context)
            dwellManager.startTracking()
        }
    }

    fun setDwellThreshold(seconds: Int) {
        settingsRepo.updateDwellThreshold(seconds)
    }

    fun setGeofenceRadius(meters: Int) {
        settingsRepo.updateGeofenceRadius(meters)
    }

    fun toggleBatterySaver(enabled: Boolean) {
        settingsRepo.toggleBatterySaver(enabled)
    }

    // --- Room Database Actions ---
    fun toggleFavoritePoi(poi: PointOfInterest) {
        viewModelScope.launch {
            travelHistoryRepo.toggleFavorite(poi, engineState.value.locationName)
        }
    }

    fun removeFavoritePoi(id: String) {
        viewModelScope.launch {
            travelHistoryRepo.removeFavorite(id)
        }
    }

    fun deleteVisitedLocation(id: String) {
        viewModelScope.launch {
            travelHistoryRepo.deleteVisitedLocation(id)
        }
    }

    fun clearVisitedHistory() {
        viewModelScope.launch {
            travelHistoryRepo.clearVisitedLocations()
            settingsRepo.clearDwellHistory()
        }
    }

    fun clearAllFavorites() {
        viewModelScope.launch {
            travelHistoryRepo.clearAllFavorites()
        }
    }

    // --- Developer Mock Location Panel Methods ---
    fun toggleMockPanelExpanded() {
        _isMockPanelExpanded.value = !_isMockPanelExpanded.value
    }

    fun setMockMode(enabled: Boolean) {
        dwellManager.setMockMode(enabled)
    }

    fun setCustomLatitude(lat: String) {
        _customLatitudeInput.value = lat
        _mockErrorMessage.value = null
    }

    fun setCustomLongitude(lng: String) {
        _customLongitudeInput.value = lng
        _mockErrorMessage.value = null
    }

    fun setCustomLocationLabel(label: String) {
        _customLocationLabel.value = label
    }

    fun selectPresetCity(preset: LocationPreset) {
        _customLatitudeInput.value = preset.latitude.toString()
        _customLongitudeInput.value = preset.longitude.toString()
        _customLocationLabel.value = preset.name
        _mockErrorMessage.value = null
        applyCustomMockLocation()
    }

    fun applyCustomMockLocation() {
        val lat = _customLatitudeInput.value.toDoubleOrNull()
        val lng = _customLongitudeInput.value.toDoubleOrNull()

        if (lat == null || lat < -90.0 || lat > 90.0) {
            _mockErrorMessage.value = "Latitude must be between -90.0 and 90.0"
            return
        }
        if (lng == null || lng < -180.0 || lng > 180.0) {
            _mockErrorMessage.value = "Longitude must be between -180.0 and 180.0"
            return
        }

        _mockErrorMessage.value = null
        val label = _customLocationLabel.value.ifBlank { "Custom Location (${lat}, ${lng})" }
        dwellManager.injectMockLocation(lat, lng, label)
    }

    fun nudgeWithinStationaryRadius() {
        dwellManager.nudgeLocationWithinGeofence()
    }

    fun jumpOutsideGeofence() {
        dwellManager.jumpLocationBreakGeofence()
    }

    fun triggerDwellImmediately() {
        dwellManager.forceTriggerDwellNow()
    }
}
