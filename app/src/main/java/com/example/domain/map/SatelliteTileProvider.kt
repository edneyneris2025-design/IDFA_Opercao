package com.example.domain.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

enum class SatelliteSource(val displayName: String, val urlTemplate: String) {
    ESRI_WORLD_IMAGERY(
        "Satélite Real (Esri Alta Resolução)",
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
    ),
    ESRI_TOPO(
        "Topográfico Militar",
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}"
    ),
    OPEN_STREET_MAP(
        "Vias & Terreno Tático",
        "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
    )
}

data class TileCoord(val z: Int, val x: Int, val y: Int)

class SatelliteTileProvider(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // 64MB memory cache for active tiles
    private val memoryCache = object : LruCache<String, Bitmap>(64 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    private val pendingDownloads = ConcurrentHashMap.newKeySet<String>()

    private val _tileVersion = MutableStateFlow(0L)
    val tileVersion: StateFlow<Long> = _tileVersion.asStateFlow()

    private val diskCacheDir = File(context.cacheDir, "satellite_tiles").apply { mkdirs() }

    /**
     * Converts WGS84 coordinates to fractional tile coordinates at given zoom level
     */
    companion object {
        fun lonToTileX(lon: Double, zoom: Int): Double {
            return (lon + 180.0) / 360.0 * (1 shl zoom)
        }

        fun latToTileY(lat: Double, zoom: Int): Double {
            val latClamped = lat.coerceIn(-85.05112878, 85.05112878)
            val latRad = Math.toRadians(latClamped)
            return (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * (1 shl zoom)
        }

        fun tileXToLon(x: Double, zoom: Int): Double {
            return x / (1 shl zoom) * 360.0 - 180.0
        }

        fun tileYToLat(y: Double, zoom: Int): Double {
            val n = PI - 2.0 * PI * y / (1 shl zoom)
            return Math.toDegrees(Math.atan(Math.sinh(n)))
        }
    }

    /**
     * Gets a tile from cache if available, or schedules asynchronous download.
     */
    fun getTile(
        coord: TileCoord,
        source: SatelliteSource = SatelliteSource.ESRI_WORLD_IMAGERY
    ): Bitmap? {
        val cacheKey = "${source.name}_${coord.z}_${coord.x}_${coord.y}"

        // 1. Check memory cache
        memoryCache.get(cacheKey)?.let { return it }

        // 2. Check disk cache
        val diskFile = File(diskCacheDir, "$cacheKey.dat")
        if (diskFile.exists() && diskFile.length() > 0) {
            try {
                val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
                if (bitmap != null) {
                    memoryCache.put(cacheKey, bitmap)
                    return bitmap
                }
            } catch (_: Exception) {}
        }

        // 3. Trigger download if not pending
        if (pendingDownloads.add(cacheKey)) {
            scope.launch(Dispatchers.IO) {
                try {
                    val url = source.urlTemplate
                        .replace("{z}", coord.z.toString())
                        .replace("{x}", coord.x.toString())
                        .replace("{y}", coord.y.toString())

                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "TacOpsAirsoft/1.0 (Android Tactical Operations)")
                        .build()

                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null && bytes.isNotEmpty()) {
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bitmap != null) {
                                memoryCache.put(cacheKey, bitmap)
                                try {
                                    FileOutputStream(diskFile).use { it.write(bytes) }
                                } catch (_: Exception) {}
                                _tileVersion.value = System.currentTimeMillis()
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Fail silently or retry later
                } finally {
                    pendingDownloads.remove(cacheKey)
                }
            }
        }

        return null
    }

    fun clearCache() {
        memoryCache.evictAll()
        scope.launch(Dispatchers.IO) {
            diskCacheDir.listFiles()?.forEach { it.delete() }
            _tileVersion.value = System.currentTimeMillis()
        }
    }
}
