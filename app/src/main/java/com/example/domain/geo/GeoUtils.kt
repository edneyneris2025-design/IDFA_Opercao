package com.example.domain.geo

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(
    val lat: Double,
    val lng: Double
)

object GeoUtils {

    /**
     * Determines whether a given coordinate point is inside a polygon using the Ray-Casting algorithm.
     */
    fun isPointInPolygon(point: GeoPoint, polygon: List<GeoPoint>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        val n = polygon.size
        var j = n - 1

        for (i in 0 until n) {
            val pi = polygon[i]
            val pj = polygon[j]

            val intersect = ((pi.lat > point.lat) != (pj.lat > point.lat)) &&
                    (point.lng < (pj.lng - pi.lng) * (point.lat - pi.lat) / (pj.lat - pi.lat) + pi.lng)

            if (intersect) {
                inside = !inside
            }
            j = i
        }

        return inside
    }

    /**
     * Calculates distance between two points in meters using the Haversine formula.
     */
    fun distanceMeters(p1: GeoPoint, p2: GeoPoint): Double {
        val r = 6371000.0 // Earth radius in meters
        val lat1Rad = Math.toRadians(p1.lat)
        val lat2Rad = Math.toRadians(p2.lat)
        val deltaLat = Math.toRadians(p2.lat - p1.lat)
        val deltaLng = Math.toRadians(p2.lng - p1.lng)

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLng / 2) * sin(deltaLng / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Calculates approximate area of a polygon in square meters.
     */
    fun calculatePolygonAreaMeters(polygon: List<GeoPoint>): Double {
        if (polygon.size < 3) return 0.0
        var total = 0.0
        val r = 6378137.0 // Earth equatorial radius

        for (i in polygon.indices) {
            val p1 = polygon[i]
            val p2 = polygon[(i + 1) % polygon.size]

            val lat1 = Math.toRadians(p1.lat)
            val lat2 = Math.toRadians(p2.lat)
            val lng1 = Math.toRadians(p1.lng)
            val lng2 = Math.toRadians(p2.lng)

            total += (lng2 - lng1) * (2 + sin(lat1) + sin(lat2))
        }

        val area = Math.abs(total * r * r / 2.0)
        return area
    }

    /**
     * Formats coordinate as Tactical Military Grid format
     */
    fun formatTacticalGrid(lat: Double, lng: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lngDir = if (lng >= 0) "E" else "W"
        val absLat = Math.abs(lat)
        val absLng = Math.abs(lng)

        val latDeg = absLat.toInt()
        val latMin = ((absLat - latDeg) * 60).toInt()
        val latSec = (((absLat - latDeg) * 60 - latMin) * 60).roundToInt()

        val lngDeg = absLng.toInt()
        val lngMin = ((absLng - lngDeg) * 60).toInt()
        val lngSec = (((absLng - lngDeg) * 60 - lngMin) * 60).roundToInt()

        return String.format(Locale.US, "%02d°%02d'%02d\"%s %03d°%02d'%02d\"%s",
            latDeg, latMin, latSec, latDir,
            lngDeg, lngMin, lngSec, lngDir)
    }

    /**
     * Parses JSON string of coordinates to List<GeoPoint>
     */
    fun parsePointsJson(json: String?): List<GeoPoint> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<GeoPoint>()
        val regex = Regex("""\{"lat":\s*([-\d.]+),\s*"lng":\s*([-\d.]+)\}""")
        regex.findAll(json).forEach { match ->
            val lat = match.groupValues[1].toDoubleOrNull() ?: 0.0
            val lng = match.groupValues[2].toDoubleOrNull() ?: 0.0
            list.add(GeoPoint(lat, lng))
        }
        return list
    }

    /**
     * Serializes List<GeoPoint> to JSON string
     */
    fun serializePointsJson(points: List<GeoPoint>): String {
        return points.joinToString(prefix = "[", postfix = "]") { p ->
            """{"lat":${p.lat},"lng":${p.lng}}"""
        }
    }
}
