package com.example.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TacOpsDatabase
import com.example.data.local.model.InventoryItemEntity
import com.example.data.local.model.OperatorEntity
import com.example.data.local.model.TacticalLogEntity
import com.example.data.local.model.TacticalZoneEntity
import com.example.data.repository.TacticalRepository
import com.example.domain.geo.GeoPoint
import com.example.domain.geo.GeoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.sin

enum class MapLayerMode {
    SATELLITE,
    FLIR_THERMAL,
    TACTICAL_GRID,
    TOPOGRAPHIC
}

class TacticalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TacticalRepository

    val operators: StateFlow<List<OperatorEntity>>
    val zones: StateFlow<List<TacticalZoneEntity>>
    val recentLogs: StateFlow<List<TacticalLogEntity>>

    private val _selectedOperatorId = MutableStateFlow<Long?>(null)
    val selectedOperatorId = _selectedOperatorId.asStateFlow()

    val selectedOperatorInventory: StateFlow<List<InventoryItemEntity>>

    // Map Center and Viewport
    val mapCenterLat = MutableStateFlow(-23.550520)
    val mapCenterLng = MutableStateFlow(-46.633308)
    val mapZoom = MutableStateFlow(16.5f) // Tactical close-up zoom
    val mapLayerMode = MutableStateFlow(MapLayerMode.SATELLITE)

    // Real Satellite Map Tiles & Imagery
    val tileProvider: com.example.domain.map.SatelliteTileProvider =
        com.example.domain.map.SatelliteTileProvider(application, viewModelScope)
    val satelliteSource = MutableStateFlow(com.example.domain.map.SatelliteSource.ESRI_WORLD_IMAGERY)
    val isSatelliteEnabled = MutableStateFlow(true)
    val satelliteOpacity = MutableStateFlow(0.95f)

    // Side Button: Polygon Drawing Mode
    private val _isDrawingPolygon = MutableStateFlow(false)
    val isDrawingPolygon = _isDrawingPolygon.asStateFlow()

    private val _currentDraftPoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val currentDraftPoints = _currentDraftPoints.asStateFlow()

    val draftZoneName = MutableStateFlow("Zona Operacional")
    val draftZoneType = MutableStateFlow("EXCLUSION")

    // QR Code Scanner / Generator
    val isQrScannerVisible = MutableStateFlow(false)
    val qrSharePayload = MutableStateFlow<String?>(null)
    val qrScanMessage = MutableStateFlow<String?>(null)

    // Field Simulation
    val isPatrolSimulationRunning = MutableStateFlow(false)
    private var simulationJob: Job? = null

    // Respawn ticker
    private var respawnTickerJob: Job? = null

    // Alert Banner
    val latestAlertMessage = MutableStateFlow<String?>(null)

    // User Device GPS Telemetry
    val userGpsLocation = MutableStateFlow<GeoPoint?>(null)
    val gpsAccuracyMeters = MutableStateFlow<Float?>(null)
    val gpsAltitudeMeters = MutableStateFlow<Double?>(null)
    val gpsHeadingDegrees = MutableStateFlow<Float?>(null)
    val isGpsActive = MutableStateFlow(false)
    val showGpsTelemetrySheet = MutableStateFlow(false)
    val gpsFeedbackMessage = MutableStateFlow<String?>(null)

    init {
        val db = TacOpsDatabase.getDatabase(application, viewModelScope)
        repository = TacticalRepository(db.tacOpsDao(), application, viewModelScope)

        operators = repository.allOperators.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        zones = repository.allZones.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        recentLogs = repository.recentLogs.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        selectedOperatorInventory = _selectedOperatorId.flatMapLatest { id ->
            if (id != null) repository.getInventoryForOperator(id)
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        startRespawnTicker()
    }

    fun selectOperator(id: Long?) {
        _selectedOperatorId.value = id
    }

    // --- Drawing Mode Actions (Requested side button feature) ---
    fun togglePolygonDrawingMode() {
        val willDraw = !_isDrawingPolygon.value
        _isDrawingPolygon.value = willDraw
        if (!willDraw) {
            _currentDraftPoints.value = emptyList()
        }
    }

    fun addPointToDraft(point: GeoPoint) {
        val updated = _currentDraftPoints.value.toMutableList()
        updated.add(point)
        _currentDraftPoints.value = updated
    }

    fun undoLastDraftPoint() {
        val current = _currentDraftPoints.value
        if (current.isNotEmpty()) {
            _currentDraftPoints.value = current.dropLast(1)
        }
    }

    fun clearDraftPoints() {
        _currentDraftPoints.value = emptyList()
    }

    fun finishAndSavePolygon(name: String, type: String) {
        val points = _currentDraftPoints.value
        if (points.size < 3) return

        val color = when (type) {
            "EXCLUSION" -> "#FF1744" // Tactical Red for exclusion
            "STRATEGIC_OBJECTIVE" -> "#00E676" // Neon Green for objectives
            "SAFE_ZONE" -> "#FFD600" // Amber for safe zones
            else -> "#2979FF" // Blue for extraction/waypoint
        }

        val json = GeoUtils.serializePointsJson(points)
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertZone(
                TacticalZoneEntity(
                    name = name.ifBlank { "Setor Demarcado" },
                    zoneType = type,
                    pointsJson = json,
                    baseColorHex = color,
                    isBreached = false
                )
            )
            repository.reevaluateAllZones()
        }

        _currentDraftPoints.value = emptyList()
        _isDrawingPolygon.value = false
    }

    fun deleteZone(zone: TacticalZoneEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteZone(zone)
        }
    }

    // --- Operator Health & Operations ---
    fun reportHit(operatorId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.reportHit(operatorId)
        }
    }

    fun reviveOperator(operatorId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.healOrRevive(operatorId)
        }
    }

    fun addOperator(
        callsign: String,
        realName: String,
        role: String,
        squad: String,
        radio: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val centerLat = mapCenterLat.value
            val centerLng = mapCenterLng.value
            // Random offset within 50 meters
            val latOffset = (Math.random() - 0.5) * 0.0008
            val lngOffset = (Math.random() - 0.5) * 0.0008

            val op = OperatorEntity(
                callsign = callsign.ifBlank { "Operador" },
                realName = realName,
                role = role,
                squad = squad,
                healthState = "ACTIVE",
                hpPercent = 100,
                latitude = centerLat + latOffset,
                longitude = centerLng + lngOffset,
                heading = (Math.random() * 360).toFloat(),
                radioFrequency = radio.ifBlank { "CH 01 - 462.562 MHz" }
            )
            val newId = repository.insertOperator(op)

            // Add standard loadout
            repository.insertInventoryItem(
                InventoryItemEntity(
                    operatorId = newId,
                    category = "PRIMARY",
                    name = "Fuzil M4A1 / AEG",
                    quantity = 1,
                    details = "380 FPS | 0.25g BBs"
                )
            )
            repository.insertInventoryItem(
                InventoryItemEntity(
                    operatorId = newId,
                    category = "AMMO",
                    name = "Magazine Mid-Cap 140 BBs",
                    quantity = 3,
                    maxQuantity = 4,
                    details = "420 BBs total"
                )
            )
            repository.insertInventoryItem(
                InventoryItemEntity(
                    operatorId = newId,
                    category = "GEAR",
                    name = "Colete Tático Chest Rig",
                    quantity = 1,
                    details = "Cor Coyote Brown"
                )
            )

            repository.logEvent(
                callsign = op.callsign,
                type = "STATUS_CHANGE",
                message = "${op.callsign} ingressou na força de tarefa ($squad).",
                severity = "INFO"
            )
        }
    }

    fun deleteOperator(operator: OperatorEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteOperator(operator)
        }
    }

    // --- Inventory Operations ---
    fun addInventoryItem(operatorId: Long, category: String, name: String, qty: Int, details: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertInventoryItem(
                InventoryItemEntity(
                    operatorId = operatorId,
                    category = category,
                    name = name,
                    quantity = qty,
                    maxQuantity = qty,
                    details = details,
                    isEquipped = true
                )
            )
        }
    }

    fun updateItemQuantity(item: InventoryItemEntity, delta: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val newQty = (item.quantity + delta).coerceAtLeast(0)
            repository.updateInventoryItem(item.copy(quantity = newQty))
        }
    }

    fun toggleEquipped(item: InventoryItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateInventoryItem(item.copy(isEquipped = !item.isEquipped))
        }
    }

    fun deleteItem(item: InventoryItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteInventoryItem(item)
        }
    }

    // --- QR Code Reading & Generation ---
    fun generateMissionQrPayload(): String {
        val root = JSONObject()
        root.put("type", "TACOPS_MISSION")
        root.put("title", "Operação Airsoft TacOps")
        root.put("originLat", mapCenterLat.value)
        root.put("originLng", mapCenterLng.value)
        root.put("timestamp", System.currentTimeMillis())
        return root.toString()
    }

    fun generateOperatorQrPayload(operator: OperatorEntity): String {
        val root = JSONObject()
        root.put("type", "TACOPS_OPERATOR")
        root.put("callsign", operator.callsign)
        root.put("role", operator.role)
        root.put("squad", operator.squad)
        root.put("radio", operator.radioFrequency)
        root.put("hp", operator.hpPercent)
        return root.toString()
    }

    fun processQrCodeResult(qrContent: String) {
        try {
            val json = JSONObject(qrContent)
            val type = json.optString("type")
            when (type) {
                "TACOPS_MISSION" -> {
                    val lat = json.optDouble("originLat", mapCenterLat.value)
                    val lng = json.optDouble("originLng", mapCenterLng.value)
                    val title = json.optString("title", "Missão Carregada")
                    mapCenterLat.value = lat
                    mapCenterLng.value = lng
                    qrScanMessage.value = "Missão '$title' sincronizada com sucesso no mapa!"
                    viewModelScope.launch(Dispatchers.IO) {
                        repository.logEvent(
                            callsign = "QR SYNC",
                            type = "QR_SYNC",
                            message = "Missão '$title' importada via QR Code com coordenadas ($lat, $lng).",
                            severity = "SUCCESS"
                        )
                    }
                }
                "TACOPS_OPERATOR" -> {
                    val cs = json.optString("callsign", "Recruta")
                    val role = json.optString("role", "Assalto")
                    val squad = json.optString("squad", "Alpha")
                    val radio = json.optString("radio", "CH 01")
                    addOperator(cs, cs, role, squad, radio)
                    qrScanMessage.value = "Operador $cs importado e adicionado à equipe $squad!"
                }
                else -> {
                    qrScanMessage.value = "QR Code lido: $qrContent"
                }
            }
        } catch (_: Exception) {
            qrScanMessage.value = "Código lido: $qrContent"
        }
    }

    // --- Real-time Field Telemetry & Simulation ---
    fun toggleFieldPatrolSimulation() {
        val current = isPatrolSimulationRunning.value
        if (current) {
            isPatrolSimulationRunning.value = false
            simulationJob?.cancel()
        } else {
            isPatrolSimulationRunning.value = true
            startPatrolSimulation()
        }
    }

    private fun startPatrolSimulation() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch(Dispatchers.IO) {
            var step = 0
            while (isPatrolSimulationRunning.value) {
                val currentOps = operators.value
                val centerLat = mapCenterLat.value
                val centerLng = mapCenterLng.value

                for ((index, op) in currentOps.withIndex()) {
                    if (op.healthState == "KIA") continue

                    // Smooth circular / patrol patrol path around mission center
                    val radius = 0.0004 + (index * 0.0002)
                    val speed = 0.05 + (index * 0.01)
                    val angle = (step * speed) + (index * 1.2)

                    val newLat = centerLat + (sin(angle) * radius)
                    val newLng = centerLng + (cos(angle) * radius * 1.2)
                    val heading = (((angle * 180 / Math.PI) + 90) % 360).toFloat()

                    repository.updateOperatorLocation(op.id, newLat, newLng, heading)
                }

                step++
                delay(1200) // Update every 1.2s for real-time smoothness
            }
        }
    }

    private fun startRespawnTicker() {
        respawnTickerJob?.cancel()
        respawnTickerJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(1000)
                val ops = operators.value
                for (op in ops) {
                    if (op.healthState == "KIA" && op.respawnTimerSeconds > 0) {
                        val newTime = op.respawnTimerSeconds - 1
                        if (newTime <= 0) {
                            // Respawned!
                            repository.healOrRevive(op.id)
                        } else {
                            repository.updateOperator(op.copy(respawnTimerSeconds = newTime))
                        }
                    }
                }
            }
        }
    }

    fun centerMapOn(lat: Double, lng: Double) {
        mapCenterLat.value = lat
        mapCenterLng.value = lng
    }

    fun updateUserGpsLocation(
        lat: Double,
        lng: Double,
        accuracy: Float? = null,
        altitude: Double? = null,
        heading: Float? = null
    ) {
        userGpsLocation.value = GeoPoint(lat, lng)
        gpsAccuracyMeters.value = accuracy
        gpsAltitudeMeters.value = altitude
        gpsHeadingDegrees.value = heading
        isGpsActive.value = true

        viewModelScope.launch(Dispatchers.IO) {
            repository.updateUserDeviceLocation(lat, lng, heading ?: 0f)
        }
    }

    /**
     * Requested feature: Side button action to immediately locate and center on user's GPS
     */
    fun centerOnMyGps(context: Context? = null) {
        val appContext = context ?: getApplication<Application>()
        try {
            if (ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val locManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val lastGps = locManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val lastNet = locManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                val best = when {
                    lastGps != null && lastNet != null -> {
                        if (lastGps.time >= lastNet.time) lastGps else lastNet
                    }
                    lastGps != null -> lastGps
                    else -> lastNet
                }
                if (best != null) {
                    updateUserGpsLocation(
                        lat = best.latitude,
                        lng = best.longitude,
                        accuracy = best.accuracy,
                        altitude = best.altitude,
                        heading = if (best.hasBearing()) best.bearing else null
                    )
                }
            }
        } catch (_: Exception) {}

        val gps = userGpsLocation.value
        if (gps != null) {
            mapCenterLat.value = gps.lat
            mapCenterLng.value = gps.lng
            mapZoom.value = 18.0f // Tactical zoom
            val accText = gpsAccuracyMeters.value?.let { " (±${it.toInt()}m)" } ?: ""
            gpsFeedbackMessage.value = "GPS FIX: ${GeoUtils.formatTacticalGrid(gps.lat, gps.lng)}$accText"
        } else {
            val userOp = operators.value.firstOrNull { it.isUserDevice } ?: operators.value.firstOrNull()
            if (userOp != null) {
                mapCenterLat.value = userOp.latitude
                mapCenterLng.value = userOp.longitude
                mapZoom.value = 18.0f
                userGpsLocation.value = GeoPoint(userOp.latitude, userOp.longitude)
                gpsFeedbackMessage.value = "POSIÇÃO FIXADA: ${userOp.callsign}"
            } else {
                mapZoom.value = 18.0f
                gpsFeedbackMessage.value = "POSIÇÃO TÁTICA CENTRALIZADA"
            }
        }
    }

    /**
     * Marks a tactical waypoint (e.g. rally point or base) exactly at user's current GPS location
     */
    fun markWaypointAtCurrentGps(name: String, type: String = "STRATEGIC_OBJECTIVE") {
        val gps = userGpsLocation.value ?: GeoPoint(mapCenterLat.value, mapCenterLng.value)
        val delta = 0.0003
        val points = listOf(
            GeoPoint(gps.lat - delta, gps.lng - delta),
            GeoPoint(gps.lat + delta, gps.lng - delta),
            GeoPoint(gps.lat + delta, gps.lng + delta),
            GeoPoint(gps.lat - delta, gps.lng + delta)
        )
        val zone = TacticalZoneEntity(
            name = name.ifBlank { "Ponto GPS / Rally Point" },
            zoneType = type,
            pointsJson = GeoUtils.serializePointsJson(points),
            baseColorHex = if (type == "EXCLUSION") "#FF1744" else "#00E5FF"
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertZone(zone)
            gpsFeedbackMessage.value = "PONTO TÁTICO CRIADO NO GPS: ${zone.name}"
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
        respawnTickerJob?.cancel()
    }
}
