package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.DwellRecord
import com.example.data.model.PoiCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dwell_spot_prefs", Context.MODE_PRIVATE)

    private val _dwellThreshold = MutableStateFlow(prefs.getInt(KEY_DWELL_THRESHOLD, 60))
    val dwellThreshold: StateFlow<Int> = _dwellThreshold.asStateFlow()

    private val _geofenceRadius = MutableStateFlow(prefs.getInt(KEY_GEOFENCE_RADIUS, 80))
    val geofenceRadius: StateFlow<Int> = _geofenceRadius.asStateFlow()

    private val _batterySaver = MutableStateFlow(prefs.getBoolean(KEY_BATTERY_SAVER, false))
    val batterySaver: StateFlow<Boolean> = _batterySaver.asStateFlow()

    private val _notificationSound = MutableStateFlow(prefs.getBoolean(KEY_NOTIF_SOUND, true))
    val notificationSound: StateFlow<Boolean> = _notificationSound.asStateFlow()

    private val _dwellHistory = MutableStateFlow(loadDwellHistory())
    val dwellHistory: StateFlow<List<DwellRecord>> = _dwellHistory.asStateFlow()

    fun updateDwellThreshold(seconds: Int) {
        val clamped = seconds.coerceIn(30, 300)
        prefs.edit().putInt(KEY_DWELL_THRESHOLD, clamped).apply()
        _dwellThreshold.value = clamped
    }

    fun updateGeofenceRadius(meters: Int) {
        val clamped = meters.coerceIn(30, 250)
        prefs.edit().putInt(KEY_GEOFENCE_RADIUS, clamped).apply()
        _geofenceRadius.value = clamped
    }

    fun toggleBatterySaver(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BATTERY_SAVER, enabled).apply()
        _batterySaver.value = enabled
    }

    fun toggleNotificationSound(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_SOUND, enabled).apply()
        _notificationSound.value = enabled
    }

    fun addDwellRecord(record: DwellRecord) {
        val current = _dwellHistory.value.toMutableList()
        current.add(0, record)
        val trimmed = if (current.size > 50) current.take(50) else current
        _dwellHistory.value = trimmed
        saveDwellHistory(trimmed)
    }

    fun clearDwellHistory() {
        _dwellHistory.value = emptyList()
        prefs.edit().remove(KEY_DWELL_HISTORY).apply()
    }

    private fun loadDwellHistory(): List<DwellRecord> {
        val raw = prefs.getString(KEY_DWELL_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<DwellRecord>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DwellRecord(
                        id = obj.getString("id"),
                        locationName = obj.getString("locationName"),
                        latitude = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude"),
                        timestamp = obj.getLong("timestamp"),
                        dwellSeconds = obj.getInt("dwellSeconds"),
                        poiCount = obj.getInt("poiCount"),
                        topPoiSummary = obj.getString("topPoiSummary")
                    )
                )
            }
        } catch (_: Exception) {
            // Ignore parse errors on corrupt store
        }
        return list
    }

    private fun saveDwellHistory(records: List<DwellRecord>) {
        val arr = JSONArray()
        for (r in records) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("locationName", r.locationName)
                put("latitude", r.latitude)
                put("longitude", r.longitude)
                put("timestamp", r.timestamp)
                put("dwellSeconds", r.dwellSeconds)
                put("poiCount", r.poiCount)
                put("topPoiSummary", r.topPoiSummary)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_DWELL_HISTORY, arr.toString()).apply()
    }

    companion object {
        private const val KEY_DWELL_THRESHOLD = "pref_dwell_threshold"
        private const val KEY_GEOFENCE_RADIUS = "pref_geofence_radius"
        private const val KEY_BATTERY_SAVER = "pref_battery_saver"
        private const val KEY_NOTIF_SOUND = "pref_notif_sound"
        private const val KEY_DWELL_HISTORY = "pref_dwell_history"
    }
}
