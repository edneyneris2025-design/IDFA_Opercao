package com.example.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tactical_logs")
data class TacticalLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val operatorCallsign: String,
    val eventType: String, // "ZONE_ENTER", "ZONE_EXIT", "HIT_REPORTED", "RESPAWN", "QR_SYNC", "STATUS_CHANGE"
    val message: String,
    val severity: String = "INFO" // "INFO", "WARNING", "DANGER", "SUCCESS"
)
