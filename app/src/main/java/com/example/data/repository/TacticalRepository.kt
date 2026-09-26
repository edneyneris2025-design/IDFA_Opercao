package com.example.data.repository

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.local.dao.TacOpsDao
import com.example.data.local.model.InventoryItemEntity
import com.example.data.local.model.OperatorEntity
import com.example.data.local.model.TacticalLogEntity
import com.example.data.local.model.TacticalZoneEntity
import com.example.domain.geo.GeoPoint
import com.example.domain.geo.GeoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TacticalRepository(
    private val dao: TacOpsDao,
    private val context: Context,
    private val scope: CoroutineScope
) {

    val allOperators: Flow<List<OperatorEntity>> = dao.getAllOperators()
    val allZones: Flow<List<TacticalZoneEntity>> = dao.getAllZones()
    val recentLogs: Flow<List<TacticalLogEntity>> = dao.getRecentLogs()

    fun getInventoryForOperator(operatorId: Long): Flow<List<InventoryItemEntity>> =
        dao.getInventoryForOperator(operatorId)

    suspend fun getOperatorById(id: Long): OperatorEntity? = dao.getOperatorById(id)

    suspend fun insertOperator(operator: OperatorEntity): Long = dao.insertOperator(operator)

    suspend fun updateOperator(operator: OperatorEntity) = dao.updateOperator(operator)

    suspend fun deleteOperator(operator: OperatorEntity) {
        dao.deleteOperator(operator)
        logEvent(
            callsign = operator.callsign,
            type = "OPERATOR_REMOVED",
            message = "Operador ${operator.callsign} (${operator.realName}) removido da operação.",
            severity = "WARNING"
        )
        reevaluateAllZones()
    }

    suspend fun insertInventoryItem(item: InventoryItemEntity): Long = dao.insertInventoryItem(item)

    suspend fun updateInventoryItem(item: InventoryItemEntity) = dao.updateInventoryItem(item)

    suspend fun deleteInventoryItem(item: InventoryItemEntity) = dao.deleteInventoryItem(item)

    suspend fun insertZone(zone: TacticalZoneEntity): Long {
        val id = dao.insertZone(zone)
        logEvent(
            callsign = "COMANDO",
            type = "ZONE_CREATED",
            message = "Nova área delimitada no mapa: ${zone.name} (${zone.zoneType})",
            severity = "SUCCESS"
        )
        return id
    }

    suspend fun updateZone(zone: TacticalZoneEntity) = dao.updateZone(zone)

    suspend fun deleteZone(zone: TacticalZoneEntity) {
        dao.deleteZone(zone)
        logEvent(
            callsign = "COMANDO",
            type = "ZONE_DELETED",
            message = "Área removida do mapa tático: ${zone.name}",
            severity = "INFO"
        )
    }

    suspend fun insertLog(log: TacticalLogEntity) = dao.insertLog(log)

    suspend fun clearLogs() = dao.clearLogs()

    /**
     * Updates an operator's health status and logs tactical hit/medic event
     */
    suspend fun reportHit(operatorId: Long) {
        val op = dao.getOperatorById(operatorId) ?: return
        val newHits = op.hitsTaken + 1
        val newHp = (op.hpPercent - 50).coerceAtLeast(0)
        val newState = when {
            newHp == 0 -> "KIA"
            newHp <= 50 -> "WOUNDED"
            else -> "COMBAT"
        }
        val respawnSecs = if (newState == "KIA") 180 else 0 // 3 min default respawn timer for airsoft

        dao.updateOperator(
            op.copy(
                hitsTaken = newHits,
                hpPercent = newHp,
                healthState = newState,
                respawnTimerSeconds = respawnSecs
            )
        )

        val severity = if (newState == "KIA") "DANGER" else "WARNING"
        logEvent(
            callsign = op.callsign,
            type = "HIT_REPORTED",
            message = "TIRO CONFIRMADO! ${op.callsign} atingido (${newHp}% HP restante). Estado: $newState",
            severity = severity
        )

        triggerHapticAlert(if (newState == "KIA") 600 else 300)
    }

    suspend fun healOrRevive(operatorId: Long) {
        val op = dao.getOperatorById(operatorId) ?: return
        dao.updateOperator(
            op.copy(
                healthState = "ACTIVE",
                hpPercent = 100,
                respawnTimerSeconds = 0
            )
        )
        logEvent(
            callsign = op.callsign,
            type = "STATUS_CHANGE",
            message = "${op.callsign} foi reanimado pelo médico de campo. Status: 100% OPERACIONAL.",
            severity = "SUCCESS"
        )
    }

    suspend fun getUserDeviceOperatorOnce(): OperatorEntity? = dao.getUserDeviceOperatorOnce()

    suspend fun updateUserDeviceLocation(
        lat: Double,
        lng: Double,
        heading: Float = 0f
    ) {
        val userOp = dao.getUserDeviceOperatorOnce()
        if (userOp != null) {
            updateOperatorLocation(userOp.id, lat, lng, heading)
        }
    }

    /**
     * Updates location of an operator and triggers Geofencing Polygon calculations!
     */
    suspend fun updateOperatorLocation(
        operatorId: Long,
        lat: Double,
        lng: Double,
        heading: Float = 0f
    ) {
        val op = dao.getOperatorById(operatorId) ?: return
        dao.updateOperatorLocation(
            id = operatorId,
            lat = lat,
            lng = lng,
            heading = heading,
            epoch = System.currentTimeMillis()
        )

        // Evaluate all zones for entry/exit
        checkZoneGeofences(op.copy(latitude = lat, longitude = lng, heading = heading))
    }

    /**
     * Checks if the operator entered or exited any tactical polygon
     */
    private suspend fun checkZoneGeofences(operator: OperatorEntity) {
        val zones = dao.getAllZones().first()
        val opPoint = GeoPoint(operator.latitude, operator.longitude)

        for (zone in zones) {
            val polygonPoints = GeoUtils.parsePointsJson(zone.pointsJson)
            if (polygonPoints.size < 3) continue

            val isInsideNow = GeoUtils.isPointInPolygon(opPoint, polygonPoints)
            val currentBreachedList = zone.breachedByCallsigns.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toMutableSet()

            val wasInside = currentBreachedList.contains(operator.callsign)

            if (isInsideNow && !wasInside) {
                // Operator just ENTERED the zone!
                currentBreachedList.add(operator.callsign)
                val updatedBreachedList = currentBreachedList.joinToString(",")

                val message = when (zone.zoneType) {
                    "EXCLUSION" -> "ALERTA PERÍMETRO: ${operator.callsign} ENTROU na [${zone.name}]!"
                    "STRATEGIC_OBJECTIVE" -> "OBJETIVO: ${operator.callsign} OCULTOU/DOMINOU a área [${zone.name}]!"
                    "SAFE_ZONE" -> "SAFE ZONE: ${operator.callsign} entrou na zona segura [${zone.name}]. Travar armas!"
                    else -> "MOVIMENTO: ${operator.callsign} ingressou em [${zone.name}]."
                }

                val severity = when (zone.zoneType) {
                    "EXCLUSION" -> "DANGER"
                    "STRATEGIC_OBJECTIVE" -> "SUCCESS"
                    else -> "INFO"
                }

                dao.updateZone(
                    zone.copy(
                        isBreached = true,
                        breachedByCallsigns = updatedBreachedList
                    )
                )

                logEvent(
                    callsign = operator.callsign,
                    type = "ZONE_ENTER",
                    message = message,
                    severity = severity
                )

                if (zone.zoneType == "EXCLUSION") {
                    triggerHapticAlert(500)
                }

            } else if (!isInsideNow && wasInside) {
                // Operator just EXITED the zone!
                currentBreachedList.remove(operator.callsign)
                val updatedBreachedList = currentBreachedList.joinToString(",")
                val isStillBreached = currentBreachedList.isNotEmpty()

                dao.updateZone(
                    zone.copy(
                        isBreached = isStillBreached,
                        breachedByCallsigns = updatedBreachedList
                    )
                )

                logEvent(
                    callsign = operator.callsign,
                    type = "ZONE_EXIT",
                    message = "${operator.callsign} SAIU da área delimitada [${zone.name}].",
                    severity = "INFO"
                )
            }
        }
    }

    /**
     * Checks all operators against all zones (e.g. after adding a new polygon or batch update)
     */
    suspend fun reevaluateAllZones() {
        val operators = dao.getAllOperators().first()
        val zones = dao.getAllZones().first()

        for (zone in zones) {
            val polygonPoints = GeoUtils.parsePointsJson(zone.pointsJson)
            if (polygonPoints.size < 3) continue

            val operatorsInside = mutableListOf<String>()
            for (op in operators) {
                if (GeoUtils.isPointInPolygon(GeoPoint(op.latitude, op.longitude), polygonPoints)) {
                    operatorsInside.add(op.callsign)
                }
            }

            val isBreached = operatorsInside.isNotEmpty()
            val listStr = operatorsInside.joinToString(",")
            if (zone.isBreached != isBreached || zone.breachedByCallsigns != listStr) {
                dao.updateZone(
                    zone.copy(
                        isBreached = isBreached,
                        breachedByCallsigns = listStr
                    )
                )
            }
        }
    }

    suspend fun logEvent(
        callsign: String,
        type: String,
        message: String,
        severity: String = "INFO"
    ) {
        dao.insertLog(
            TacticalLogEntity(
                timestamp = System.currentTimeMillis(),
                operatorCallsign = callsign,
                eventType = type,
                message = message,
                severity = severity
            )
        )
    }

    private fun triggerHapticAlert(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {}
    }
}
