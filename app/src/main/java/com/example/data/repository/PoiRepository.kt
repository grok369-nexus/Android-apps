package com.example.data.repository

import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.example.data.model.PoiCategory
import com.example.data.model.PointOfInterest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PoiRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Resolves human-readable locality name for given coordinates using Android Geocoder,
     * with Nominatim OSM reverse geocoding as fallback.
     */
    suspend fun resolveLocationName(context: Context, latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        // 1. Try Android Geocoder first
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val subLocality = addr.subLocality
                    val locality = addr.locality
                    val adminArea = addr.subAdminArea ?: addr.adminArea
                    val country = addr.countryName

                    val parts = listOfNotNull(subLocality, locality ?: adminArea, country).distinct()
                    if (parts.isNotEmpty()) {
                        return@withContext parts.joinToString(", ")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("PoiRepository", "Geocoder reverse lookup failed: ${e.message}")
        }

        // 2. Try OpenStreetMap Nominatim reverse geocoding
        try {
            val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude&zoom=16&addressdetails=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "DwellSpotTraveler/1.0 (Mobile Android)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val addressObj = json.optJSONObject("address")
                    if (addressObj != null) {
                        val suburb = addressObj.optString("suburb", "")
                        val city = addressObj.optString("city", addressObj.optString("town", addressObj.optString("county", "")))
                        val state = addressObj.optString("state", "")
                        val country = addressObj.optString("country", "")

                        val candidate = listOf(suburb, city, country)
                            .filter { it.isNotBlank() }
                            .distinct()

                        if (candidate.isNotEmpty()) {
                            return@withContext candidate.joinToString(", ")
                        }
                    }
                    val displayName = json.optString("display_name", "")
                    if (displayName.isNotBlank()) {
                        val firstParts = displayName.split(",").take(3).map { it.trim() }
                        return@withContext firstParts.joinToString(", ")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("PoiRepository", "Nominatim reverse lookup failed: ${e.message}")
        }

        // 3. Coordinate-aware fallback
        if (latitude in -1.5..4.5 && longitude in 29.5..35.2) {
            if (latitude in 0.3..0.45 && longitude in 32.5..32.65) {
                return@withContext "Kawempe, Kampala, Uganda"
            }
            return@withContext "Kampala, Uganda"
        }

        return@withContext String.format(Locale.US, "Location (%.4f, %.4f)", latitude, longitude)
    }

    /**
     * Fetches real nearby places from OpenStreetMap.
     * If the source is unavailable, return no results rather than inventing places.
     */
    suspend fun fetchNearbyPois(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 1200,
        resolvedLocationName: String = ""
    ): List<PointOfInterest> = withContext(Dispatchers.IO) {
        try {
            fetchFromOverpass(latitude, longitude, radiusMeters)
                .sortedBy { it.distanceMeters }
        } catch (e: Exception) {
            Log.w("PoiRepository", "Overpass API fetch failed: ${e.message}")
            emptyList()
        }
    }

    private fun fetchFromOverpass(lat: Double, lon: Double, radius: Int): List<PointOfInterest> {
        // Query both nodes and ways (nwr), with center tags for polygon buildings/grounds
        val query = """
            [out:json][timeout:12];
            (
              nwr["amenity"~"restaurant|cafe|bar|pub|fast_food|place_of_worship|marketplace|hospital|clinic|pharmacy|fuel|bank"](around:$radius,$lat,$lon);
              nwr["leisure"~"park|pitch|sports_centre|garden|playground"](around:$radius,$lat,$lon);
              nwr["historic"~"monument|memorial|ruins"](around:$radius,$lat,$lon);
              nwr["tourism"~"attraction|museum|hotel|guest_house|viewpoint"](around:$radius,$lat,$lon);
              nwr["shop"~"supermarket|convenience|mall|bakery|general|butcher"](around:$radius,$lat,$lon);
            );
            out center 35;
        """.trimIndent()

        val url = "https://overpass-api.de/api/interpreter?data=" + java.net.URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "DwellSpotMobile/1.0 (Android)")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()

        val body = response.body?.string() ?: return emptyList()
        val json = JSONObject(body)
        val elements = json.optJSONArray("elements") ?: return emptyList()

        val list = mutableListOf<PointOfInterest>()
        for (i in 0 until elements.length()) {
            val el = elements.getJSONObject(i)
            val tags = el.optJSONObject("tags") ?: continue
            val name = tags.optString("name")
            if (name.isBlank()) continue

            // Support both direct node coordinates and way center points
            val nodeLat = if (el.has("center")) {
                el.getJSONObject("center").optDouble("lat", lat)
            } else {
                el.optDouble("lat", lat)
            }
            val nodeLon = if (el.has("center")) {
                el.getJSONObject("center").optDouble("lon", lon)
            } else {
                el.optDouble("lon", lon)
            }

            val distance = calculateDistanceMeters(lat, lon, nodeLat, nodeLon)
            val (category, subType) = classifyOsmTags(tags)

            list.add(
                PointOfInterest(
                    id = "osm_${el.optLong("id")}",
                    name = name,
                    category = category,
                    subType = subType,
                    latitude = nodeLat,
                    longitude = nodeLon,
                    distanceMeters = distance,
                    rating = 0.0,
                    address = buildOsmAddress(tags),
                    isOpenNow = false,
                    summary = "Live point discovered via OpenStreetMap"
                )
            )
        }
        return list
    }

    private fun classifyOsmTags(tags: JSONObject): Pair<PoiCategory, String> {
        val amenity = tags.optString("amenity")
        val leisure = tags.optString("leisure")
        val tourism = tags.optString("tourism")
        val historic = tags.optString("historic")
        val shop = tags.optString("shop")

        return when {
            amenity == "place_of_worship" -> {
                val religion = tags.optString("religion", "Worship")
                val denomination = tags.optString("denomination", "")
                val label = if (denomination.isNotBlank()) "$religion ($denomination)" else religion.replaceFirstChar { it.uppercase() }
                Pair(PoiCategory.WORSHIP, label)
            }
            amenity in listOf("restaurant", "cafe", "fast_food", "food_court", "bistro") -> {
                val cuisine = tags.optString("cuisine", amenity.replaceFirstChar { it.uppercase() })
                Pair(PoiCategory.FOOD, cuisine.replaceFirstChar { it.uppercase() })
            }
            amenity in listOf("nightclub", "bar", "pub") || leisure in listOf("park", "pitch", "sports_centre", "garden", "dance", "playground") -> {
                val type = if (amenity.isNotBlank()) amenity else leisure
                Pair(PoiCategory.ENJOYMENT, type.replaceFirstChar { it.uppercase() })
            }
            amenity in listOf("marketplace", "fuel", "hospital", "clinic", "pharmacy", "bank") -> {
                Pair(PoiCategory.OTHER, amenity.replaceFirstChar { it.uppercase() })
            }
            historic.isNotBlank() || tourism in listOf("attraction", "museum", "artwork", "viewpoint", "hotel", "guest_house") -> {
                val type = if (historic.isNotBlank()) "Historic $historic" else tourism
                Pair(PoiCategory.OTHER, type.replaceFirstChar { it.uppercase() })
            }
            shop.isNotBlank() -> {
                Pair(PoiCategory.OTHER, "Shopping: " + shop.replaceFirstChar { it.uppercase() })
            }
            else -> Pair(PoiCategory.OTHER, "Point of Interest")
        }
    }

    private fun buildOsmAddress(tags: JSONObject): String {
        val street = tags.optString("addr:street")
        val housenumber = tags.optString("addr:housenumber")
        val city = tags.optString("addr:city")
        val suburb = tags.optString("addr:suburb")
        return when {
            street.isNotBlank() && housenumber.isNotBlank() -> "$housenumber $street, ${suburb.ifBlank { city }}".trimEnd(',', ' ')
            street.isNotBlank() -> "$street, ${suburb.ifBlank { city }}".trimEnd(',', ' ')
            suburb.isNotBlank() -> suburb
            city.isNotBlank() -> city
            else -> "Local vicinity"
        }
    }

    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadius * c).toInt()
    }
}
