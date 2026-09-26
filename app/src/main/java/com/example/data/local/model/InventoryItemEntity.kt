package com.example.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_items",
    foreignKeys = [
        ForeignKey(
            entity = OperatorEntity::class,
            parentColumns = ["id"],
            childColumns = ["operatorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["operatorId"])]
)
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val operatorId: Long,
    val category: String, // "PRIMARY", "SECONDARY", "GEAR", "AMMO", "MEDICAL", "TACTICAL"
    val name: String,
    val quantity: Int,
    val maxQuantity: Int = 1,
    val details: String = "",
    val isEquipped: Boolean = true
)
