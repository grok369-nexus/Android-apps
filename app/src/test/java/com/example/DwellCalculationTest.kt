package com.example

import com.example.data.repository.PoiRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DwellCalculationTest {

    private val repo = PoiRepository()

    @Test
    fun testHaversineDistanceAccuracy() {
        // Distance between Eiffel Tower (48.8584, 2.2945) and Champ de Mars center (48.8558, 2.2980) ~ 380m
        val dist = repo.calculateDistanceMeters(48.8584, 2.2945, 48.8558, 2.2980)
        assertTrue("Distance should be between 300m and 500m, was $dist", dist in 300..500)
    }

    @Test
    fun testZeroDistanceForSameCoordinates() {
        val dist = repo.calculateDistanceMeters(40.7128, -74.0060, 40.7128, -74.0060)
        assertEquals(0, dist)
    }

    @Test
    fun testKawempeToKampalaDistance() {
        // Kawempe (~0.3676, 32.5562) to Kampala Central (~0.3163, 32.5822) ~ 6.4 km
        val dist = repo.calculateDistanceMeters(0.3676, 32.5562, 0.3163, 32.5822)
        assertTrue("Distance between Kawempe and Kampala should be ~5-8km, was $dist", dist in 5000..8000)
    }
}
