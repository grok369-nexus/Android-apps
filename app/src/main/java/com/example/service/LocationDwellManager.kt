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
    val isTracking: Boolean = true, // Auto-start live tracking
    val isDwelling: Boolean = false,
    val currentDwellSeconds: Int = 0,
    val thresholdSeconds: Int = 60,
    val geofenceRadiusMeters: Int = 80,
    val latitude: Double = 0.3676, // Default baseline to Kawempe, Kampala, Uganda
    val longitude: Double = 32.5562,
    val locationName: String = "Kawempe, Kampala, Uganda",
    val distanceToAnchorMeters: Int = 0,
    val isDwellTriggered: Boolean = false,
    val lastTriggeredTimeMillis: Long = 0L,
    val pois: List<PointOfInterest> = emptyList(),
    val isLoadingPois: Boolean = false,
    val isSimulated: Boolean = false,
    val isMockMode: Boolean = false, // LIVE GPS IS DEFAULT (Not assumed or mock)
    val gpsStatusMessage: String = "Connecting to Live GPS..."
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

    private var anchorLat: Double? = 0.3676
    private var anchorLng: Double? = 32.5562
    private var dwellStartTimeMs: Long = System.currentTimeMillis()
    private var isTriggeredForCurrentAnchor: Boolean = false
    private var tickerJob: Job? = null
    private var locationCallback: LocationCallback? = null

    init {
        val currentThreshold = settingsRepository.dwellThreshold.value
        val currentRadius = settingsRepository.geofenceRadius.value
        _engineState.value = _engineState.value.copy(
            thresholdSeconds = currentThreshold,
            geofenceRadiusMeters = currentRadius
        )

        // Start live tracking immediately
        startDwellTicker()
        requestLocationUpdates()
        refreshLiveLocation()

        // Fetch initial POIs around baseline
        scope.launch {
            _engineState.value = _engineState.value.copy(isLoadingPois = true)
            val initialPois = poiRepository.fetchNearbyPois(
                0.3676,
                32.5562,
                1500,
                "Kawempe, Kampala, Uganda"
            )
            _engineState.value = _engineState.value.copy(
                pois = initialPois,
                isLoadingPois = false
            )
        }
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
            gpsStatusMessage = "Tracking Paused"
        )
        tickerJob?.cancel()
        tickerJob = null
        stopLocationUpdates()
    }

    /**
     * Actively queries Android FusedLocationProviderClient for immediate real GPS fix.
     */
    @SuppressLint("MissingPermission")
    fun refreshLiveLocation() {
        if (_engineState.value.isMockMode) return

        try {
            _engineState.value = _engineState.value.copy(gpsStatusMessage = "Acquiring satellite lock...")

            // 1. Try last known location first for immediate responsiveness
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && !_engineState.value.isMockMode) {
                    onNewLocation(loc.latitude, loc.longitude, isSimulated = false)
                }
            }

            // 2. Request fresh high-accuracy current location fix
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null && !_engineState.value.isMockMode) {
                        onNewLocation(loc.latitude, loc.longitude, isSimulated = false)
                    } else {
                        _engineState.value = _engineState.value.copy(
                            gpsStatusMessage = "Live GPS Active (Waiting for motion)"
                        )
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("LocationDwellManager", "Failed to get current location: ${e.message}")
                    _engineState.value = _engineState.value.copy(
                        gpsStatusMessage = "GPS Signal Pending"
                    )
                }
        } catch (e: SecurityException) {
            Log.w("LocationDwellManager", "Permission missing for location refresh: ${e.message}")
            _engineState.value = _engineState.value.copy(
                gpsStatusMessage = "Location Permission Needed"
            )
        }
    }

    /**
     * Toggles Mock Mode. When disabled (default), binds to real GPS hardware.
     */
    fun setMockMode(enabled: Boolean) {
        _engineState.value = _engineState.value.copy(
            isMockMode = enabled,
            isSimulated = enabled,
            gpsStatusMessage = if (enabled) "Developer Mock GPS Active" else "Live GPS Active"
        )
        if (enabled) {
            stopLocationUpdates()
        } else {
            requestLocationUpdates()
            refreshLiveLocation()
        }
    }

    /**
     * Jump directly to Kawempe, Kampala, Uganda coordinates.
     */
    fun jumpToKawempeUganda() {
        onNewLocation(0.3676, 32.5562, "Kawempe, Kampala, Uganda", isSimulated = false)
        if (!_engineState.value.isTracking) {
            startTracking()
        }
    }

    /**
     * Manually injects custom Latitude and Longitude coordinates.
     */
    fun injectMockLocation(lat: Double, lng: Double, locationName: String = "Custom Point") {
        if (!_engineState.value.isMockMode) {
            setMockMode(true)
        }
        onNewLocation(lat, lng, locationName, isSimulated = true)
        if (!_engineState.value.isTracking) {
            startTracking()
        }
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
        if (!_engineState.value.isTracking) {
            startTracking()
        }
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

            // 1. Save to Room Database
            travelHistoryRepository.recordVisitedLocation(
                locationName = state.locationName,
                latitude = state.latitude,
                longitude = state.longitude,
                dwellSeconds = state.currentDwellSeconds,
                poiCount = pois.size,
                topPoiSummary = topSummary
            )

            // 2. Keep SharedPreferences backup
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

            // 3. Trigger Rich Push Notification
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

        // Resolve real address asynchronously if not explicitly provided
        scope.launch {
            val resolvedName = if (!locationName.isNullOrBlank() && !locationName.contains("GPS Location")) {
                locationName
            } else {
                poiRepository.resolveLocationName(appContext, lat, lng)
            }

            if (currentAnchorLat == null || currentAnchorLng == null) {
                // First anchor fix
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
                    gpsStatusMessage = if (isSimulated) "Mock Location Set" else "Live GPS Locked"
                )

                // Fetch real POIs around this new location immediately
                val pois = poiRepository.fetchNearbyPois(lat, lng, 1500, resolvedName)
                _engineState.value = _engineState.value.copy(pois = pois)
                return@launch
            }

            val distance = poiRepository.calculateDistanceMeters(currentAnchorLat, currentAnchorLng, lat, lng)

            if (distance <= radius) {
                // User is still within stationary geofence
                _engineState.value = _engineState.value.copy(
                    latitude = lat,
                    longitude = lng,
                    locationName = resolvedName,
                    distanceToAnchorMeters = distance,
                    gpsStatusMessage = if (isSimulated) "Mock Location Active" else "Live GPS Tracking"
                )
            } else {
                // User departed geofence -> Reset dwell timer to new location
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
                    gpsStatusMessage = if (isSimulated) "New Mock Spot" else "New Location Detected"
                )

                // Fetch new POIs for newly entered area
                val pois = poiRepository.fetchNearbyPois(lat, lng, 1500, resolvedName)
                _engineState.value = _engineState.value.copy(pois = pois)
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
            _engineState.value = _engineState.value.copy(gpsStatusMessage = "Listening to Live GPS...")
        } catch (e: SecurityException) {
            Log.w("LocationDwellManager", "Location permission not yet granted: ${e.message}")
            _engineState.value = _engineState.value.copy(gpsStatusMessage = "Waiting for Permission")
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
