package com.example.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tactical_zones")
data class TacticalZoneEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val zoneType: String, // "EXCLUSION", "STRATEGIC_OBJECTIVE", "SAFE_ZONE", "EXTRACTION_POINT"
    val pointsJson: String, // serialized List<PointD>
    val baseColorHex: String = "#FF1744",
    val isBreached: Boolean = false,
    val breachedByCallsigns: String = "",
    val createdTimestamp: Long = System.currentTimeMillis()
)
