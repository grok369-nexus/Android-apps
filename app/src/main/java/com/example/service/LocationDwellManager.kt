package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.DwellRecord
import com.example.data.model.PointOfInterest
import com.example.data.repository.PoiRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TravelHistoryRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class DwellEngineState(
    val isTracking: Boolean = false,
    val isDwelling: Boolean = false,
    val currentDwellSeconds: Int = 0,
    val thresholdSeconds: Int = 60,
    val geofenceRadiusMeters: Int = 80,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = "Your area",
    val distanceToAnchorMeters: Int = 0,
    val isDwellTriggered: Boolean = false,
    val lastTriggeredTimeMillis: Long = 0L,
    val pois: List<PointOfInterest> = emptyList(),
    val isLoadingPois: Boolean = false,
    val isSimulated: Boolean = false,
    val isMockMode: Boolean = false,
    val gpsStatusMessage: String = "Location not checked yet"
)

class LocationDwellManager private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(appContext)

    private val poiRepository = PoiRepository()
    val settingsRepository = SettingsRepository(appContext)
    val travelHistoryRepository = TravelHistoryRepository(
        AppDatabase.getInstance(appContext).travelHistoryDao()
    )

    private val _engineState = MutableStateFlow(DwellEngineState())
    val engineState: StateFlow<DwellEngineState> = _engineState.asStateFlow()

    private var anchorLat: Double? = null
    private var anchorLng: Double? = null
    private var dwellStartTimeMs: Long = System.currentTimeMillis()
    private var isTriggeredForCurrentAnchor: Boolean = false
    private var tickerJob: Job? = null
    private var locationCallback: LocationCallback? = null

    init {
        _engineState.value = _engineState.value.copy(
            thresholdSeconds = settingsRepository.dwellThreshold.value,
            geofenceRadiusMeters = settingsRepository.geofenceRadius.value
        )
        // Location is requested only after permission is granted.
        // Background discovery starts only after the user opts in.
    }

    fun startTracking() {
        if (_engineState.value.isTracking) return
        _engineState.value = _engineState.value.copy(isTracking = true)
        startDwellTicker()
        if (!_engineState.value.isMockMode) {
            requestLocationUpdates()
            refreshLiveLocation()
        }
    }

    fun stopTracking() {
        _engineState.value = _engineState.value.copy(
            isTracking = false,
            isDwelling = false,
            currentDwellSeconds = 0,
            gpsStatusMessage = "Automatic discovery is paused"
        )
        tickerJob?.cancel()
        tickerJob = null
        stopLocationUpdates()
    }

    /** Gets a one-time location fix for foreground discovery without starting background tracking. */
    @SuppressLint("MissingPermission")
    fun refreshLiveLocation() {
        if (_engineState.value.isMockMode) return

        try {
            _engineState.value = _engineState.value.copy(gpsStatusMessage = "Finding your location...")
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && !_engineState.value.isMockMode) {
                    onNewLocation(loc.latitude, loc.longitude, isSimulated = false)
                }
            }
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null && !_engineState.value.isMockMode) {
                        onNewLocation(loc.latitude, loc.longitude, isSimulated = false)
                    } else {
                        _engineState.value = _engineState.value.copy(
                            gpsStatusMessage = "Location is unavailable. Try again."
                        )
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("LocationDwellManager", "Failed to get current location: ${e.message}")
                    _engineState.value = _engineState.value.copy(
                        gpsStatusMessage = "Couldn't find your location. Try again."
                    )
                }
        } catch (e: SecurityException) {
            Log.w("LocationDwellManager", "Location permission missing: ${e.message}")
            _engineState.value = _engineState.value.copy(
                gpsStatusMessage = "Location permission is needed"
            )
        }
    }

    fun setMockMode(enabled: Boolean) {
        _engineState.value = _engineState.value.copy(
            isMockMode = enabled,
            isSimulated = enabled,
            gpsStatusMessage = if (enabled) "Preview location active" else "Finding your location..."
        )
        if (enabled) {
            stopLocationUpdates()
        } else {
            refreshLiveLocation()
            if (_engineState.value.isTracking) requestLocationUpdates()
        }
    }

    fun jumpToKawempeUganda() {
        onNewLocation(0.3676, 32.5562, "Kawempe, Kampala, Uganda", isSimulated = true)
    }

    fun injectMockLocation(lat: Double, lng: Double, locationName: String = "Preview location") {
        if (!_engineState.value.isMockMode) setMockMode(true)
        onNewLocation(lat, lng, locationName, isSimulated = true)
    }

    fun nudgeLocationWithinGeofence() {
        val currentLat = _engineState.value.latitude
        val currentLng = _engineState.value.longitude
        onNewLocation(currentLat + 0.0001, currentLng, _engineState.value.locationName, isSimulated = true)
    }

    fun jumpLocationBreakGeofence() {
        val currentLat = _engineState.value.latitude
        val currentLng = _engineState.value.longitude
        onNewLocation(currentLat + 0.002, currentLng, "${_engineState.value.locationName} (Departed)", isSimulated = true)
    }

    fun forceTriggerDwellNow() {
        if (!_engineState.value.isTracking) startTracking()
        dwellStartTimeMs = System.currentTimeMillis() - (settingsRepository.dwellThreshold.value * 1000L)
        onDwellThresholdReached()
    }

    private fun startDwellTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000)
                if (_engineState.value.isTracking && anchorLat != null) {
                    val elapsedSeconds = ((System.currentTimeMillis() - dwellStartTimeMs) / 1000).toInt()
                    val threshold = settingsRepository.dwellThreshold.value
                    _engineState.value = _engineState.value.copy(
                        currentDwellSeconds = elapsedSeconds,
                        isDwelling = true
                    )
                    if (elapsedSeconds >= threshold && !isTriggeredForCurrentAnchor) {
                        onDwellThresholdReached()
                    }
                }
            }
        }
    }

    private fun onDwellThresholdReached() {
        isTriggeredForCurrentAnchor = true
        val state = _engineState.value
        _engineState.value = state.copy(
            isDwellTriggered = true,
            lastTriggeredTimeMillis = System.currentTimeMillis()
        )

        scope.launch {
            _engineState.value = _engineState.value.copy(isLoadingPois = true)
            val pois = poiRepository.fetchNearbyPois(
                state.latitude,
                state.longitude,
                1500,
                state.locationName
            )
            _engineState.value = _engineState.value.copy(pois = pois, isLoadingPois = false)
            val topSummary = pois.take(3).joinToString(" • ") { it.name }

            travelHistoryRepository.recordVisitedLocation(
                locationName = state.locationName,
                latitude = state.latitude,
                longitude = state.longitude,
                dwellSeconds = state.currentDwellSeconds,
                poiCount = pois.size,
                topPoiSummary = topSummary
            )

            val record = DwellRecord(
                id = UUID.randomUUID().toString(),
                locationName = state.locationName,
                latitude = state.latitude,
                longitude = state.longitude,
                timestamp = System.currentTimeMillis(),
                dwellSeconds = state.currentDwellSeconds,
                poiCount = pois.size,
                topPoiSummary = topSummary
            )
            settingsRepository.addDwellRecord(record)

            NotificationHelper.showDwellDiscoveryNotification(
                context = appContext,
                locationName = state.locationName,
                dwellSeconds = state.currentDwellSeconds,
                pois = pois
            )
        }
    }

    fun onNewLocation(
        lat: Double,
        lng: Double,
        locationName: String? = null,
        isSimulated: Boolean = false
    ) {
        val currentAnchorLat = anchorLat
        val currentAnchorLng = anchorLng
        val radius = settingsRepository.geofenceRadius.value

        scope.launch {
            val resolvedName = if (!locationName.isNullOrBlank() && !locationName.contains("GPS Location")) {
                locationName
            } else {
                poiRepository.resolveLocationName(appContext, lat, lng)
            }

            if (currentAnchorLat == null || currentAnchorLng == null) {
                anchorLat = lat
                anchorLng = lng
                dwellStartTimeMs = System.currentTimeMillis()
                isTriggeredForCurrentAnchor = false
                _engineState.value = _engineState.value.copy(
                    latitude = lat,
                    longitude = lng,
                    locationName = resolvedName,
                    distanceToAnchorMeters = 0,
                    currentDwellSeconds = 0,
                    isDwellTriggered = false,
                    isSimulated = isSimulated,
                    gpsStatusMessage = if (isSimulated) "Preview location" else "Location found"
                )
                val pois = poiRepository.fetchNearbyPois(lat, lng, 1500, resolvedName)
                _engineState.value = _engineState.value.copy(pois = pois, isLoadingPois = false)
                return@launch
            }

            val distance = poiRepository.calculateDistanceMeters(currentAnchorLat, currentAnchorLng, lat, lng)
            if (distance <= radius) {
                _engineState.value = _engineState.value.copy(
                    latitude = lat,
                    longitude = lng,
                    locationName = resolvedName,
                    distanceToAnchorMeters = distance,
                    gpsStatusMessage = if (isSimulated) "Preview location" else "Location updated"
                )
            } else {
                anchorLat = lat
                anchorLng = lng
                dwellStartTimeMs = System.currentTimeMillis()
                isTriggeredForCurrentAnchor = false
                _engineState.value = _engineState.value.copy(
                    latitude = lat,
                    longitude = lng,
                    locationName = resolvedName,
                    distanceToAnchorMeters = 0,
                    currentDwellSeconds = 0,
                    isDwellTriggered = false,
                    isSimulated = isSimulated,
                    gpsStatusMessage = if (isSimulated) "Preview location changed" else "New area found"
                )
                val pois = poiRepository.fetchNearbyPois(lat, lng, 1500, resolvedName)
                _engineState.value = _engineState.value.copy(pois = pois, isLoadingPois = false)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        if (_engineState.value.isMockMode) return
        try {
            val isBatterySaver = settingsRepository.batterySaver.value
            val intervalMs = if (isBatterySaver) 12000L else 4000L
            val minDistance = if (isBatterySaver) 25f else 5f
            val locationRequest = LocationRequest.Builder(
                if (isBatterySaver) Priority.PRIORITY_BALANCED_POWER_ACCURACY else Priority.PRIORITY_HIGH_ACCURACY,
                intervalMs
            ).setMinUpdateDistanceMeters(minDistance).build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    if (_engineState.value.isMockMode) return
                    val last = result.lastLocation ?: return
                    onNewLocation(last.latitude, last.longitude, isSimulated = false)
                }
            }
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback as LocationCallback,
                Looper.getMainLooper()
            )
            _engineState.value = _engineState.value.copy(
                gpsStatusMessage = "Automatic discovery is active"
            )
        } catch (e: SecurityException) {
            Log.w("LocationDwellManager", "Location permission not yet granted: ${e.message}")
            _engineState.value = _engineState.value.copy(
                gpsStatusMessage = "Location permission is needed"
            )
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: LocationDwellManager? = null

        fun getInstance(context: Context): LocationDwellManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationDwellManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
