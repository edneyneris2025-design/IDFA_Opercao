package com.example.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operators")
data class OperatorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val callsign: String,
    val realName: String,
    val role: String,
    val squad: String,
    val healthState: String, // "ACTIVE", "COMBAT", "WOUNDED", "KIA", "RESPAWN"
    val hpPercent: Int = 100,
    val hitsTaken: Int = 0,
    val respawnTimerSeconds: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 45.0,
    val heading: Float = 0f,
    val radioFrequency: String = "CH 01 - 462.562 MHz",
    val batteryPercent: Int = 95,
    val isUserDevice: Boolean = false,
    val lastSeenEpoch: Long = System.currentTimeMillis()
)
