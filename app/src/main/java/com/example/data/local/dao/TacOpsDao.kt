package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.model.InventoryItemEntity
import com.example.data.local.model.OperatorEntity
import com.example.data.local.model.TacticalLogEntity
import com.example.data.local.model.TacticalZoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TacOpsDao {

    // Operators
    @Query("SELECT * FROM operators ORDER BY squad ASC, callsign ASC")
    fun getAllOperators(): Flow<List<OperatorEntity>>

    @Query("SELECT * FROM operators WHERE id = :id LIMIT 1")
    suspend fun getOperatorById(id: Long): OperatorEntity?

    @Query("SELECT * FROM operators WHERE isUserDevice = 1 LIMIT 1")
    fun getUserDeviceOperator(): Flow<OperatorEntity?>

    @Query("SELECT * FROM operators WHERE isUserDevice = 1 LIMIT 1")
    suspend fun getUserDeviceOperatorOnce(): OperatorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperator(operator: OperatorEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperators(operators: List<OperatorEntity>): List<Long>

    @Update
    suspend fun updateOperator(operator: OperatorEntity)

    @Delete
    suspend fun deleteOperator(operator: OperatorEntity)

    @Query("UPDATE operators SET latitude = :lat, longitude = :lng, heading = :heading, lastSeenEpoch = :epoch WHERE id = :id")
    suspend fun updateOperatorLocation(id: Long, lat: Double, lng: Double, heading: Float, epoch: Long)

    @Query("UPDATE operators SET healthState = :state, hpPercent = :hp, hitsTaken = hitsTaken + :hitsDelta WHERE id = :id")
    suspend fun updateOperatorHealth(id: Long, state: String, hp: Int, hitsDelta: Int)

    @Query("UPDATE operators SET respawnTimerSeconds = :seconds, healthState = :state WHERE id = :id")
    suspend fun updateOperatorRespawn(id: Long, seconds: Int, state: String)

    // Inventory Items
    @Query("SELECT * FROM inventory_items WHERE operatorId = :operatorId ORDER BY category ASC, name ASC")
    fun getInventoryForOperator(operatorId: Long): Flow<List<InventoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItems(items: List<InventoryItemEntity>): List<Long>

    @Update
    suspend fun updateInventoryItem(item: InventoryItemEntity)

    @Delete
    suspend fun deleteInventoryItem(item: InventoryItemEntity)

    @Query("UPDATE inventory_items SET quantity = :quantity WHERE id = :id")
    suspend fun updateItemQuantity(id: Long, quantity: Int)

    // Tactical Zones
    @Query("SELECT * FROM tactical_zones ORDER BY createdTimestamp DESC")
    fun getAllZones(): Flow<List<TacticalZoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZone(zone: TacticalZoneEntity): Long

    @Update
    suspend fun updateZone(zone: TacticalZoneEntity)

    @Delete
    suspend fun deleteZone(zone: TacticalZoneEntity)

    @Query("DELETE FROM tactical_zones WHERE id = :id")
    suspend fun deleteZoneById(id: Long)

    // Logs
    @Query("SELECT * FROM tactical_logs ORDER BY timestamp DESC LIMIT 150")
    fun getRecentLogs(): Flow<List<TacticalLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TacticalLogEntity): Long

    @Query("DELETE FROM tactical_logs")
    suspend fun clearLogs()
}
