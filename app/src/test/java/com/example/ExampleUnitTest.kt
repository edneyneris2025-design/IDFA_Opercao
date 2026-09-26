package com.example

import com.example.domain.geo.GeoPoint
import com.example.domain.geo.GeoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testPointInPolygonDetection() {
        // Square polygon around (0,0) with size 2x2
        val square = listOf(
            GeoPoint(-1.0, -1.0),
            GeoPoint(1.0, -1.0),
            GeoPoint(1.0, 1.0),
            GeoPoint(-1.0, 1.0)
        )

        // Point inside
        assertTrue(GeoUtils.isPointInPolygon(GeoPoint(0.0, 0.0), square))
        assertTrue(GeoUtils.isPointInPolygon(GeoPoint(0.5, 0.5), square))

        // Point outside
        assertFalse(GeoUtils.isPointInPolygon(GeoPoint(2.0, 0.0), square))
        assertFalse(GeoUtils.isPointInPolygon(GeoPoint(0.0, -2.0), square))
        assertFalse(GeoUtils.isPointInPolygon(GeoPoint(5.0, 5.0), square))
    }

    @Test
    fun testSerializationAndParsing() {
        val originalPoints = listOf(
            GeoPoint(-23.55052, -46.63330),
            GeoPoint(-23.55100, -46.63400),
            GeoPoint(-23.55200, -46.63200)
        )

        val json = GeoUtils.serializePointsJson(originalPoints)
        val parsed = GeoUtils.parsePointsJson(json)

        assertEquals(originalPoints.size, parsed.size)
        assertEquals(originalPoints[0].lat, parsed[0].lat, 0.0001)
        assertEquals(originalPoints[0].lng, parsed[0].lng, 0.0001)
    }

    @Test
    fun testDistanceCalculation() {
        val p1 = GeoPoint(-23.55052, -46.63330)
        val p2 = GeoPoint(-23.55052, -46.63330)
        assertEquals(0.0, GeoUtils.distanceMeters(p1, p2), 0.001)

        val p3 = GeoPoint(-23.55052, -46.64330) // ~1km away
        val dist = GeoUtils.distanceMeters(p1, p3)
        assertTrue(dist > 900.0 && dist < 1200.0)
    }
}
