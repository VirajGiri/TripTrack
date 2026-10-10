package com.virajgiri.triptrack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Immutable invoice snapshot. Store money in paise and line items as a JSON string
 * so later rate-card edits cannot silently change an issued bill.
 */
@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val clientNameSnapshot: String,
    val tripDateSnapshot: String,
    val packageTypeSnapshot: String,
    val vehicleNumberSnapshot: String,
    val distanceKmSnapshot: Double,
    val durationMinutesSnapshot: Long,
    val lineItemsJson: String,
    val totalPaise: Long,
    val createdAtEpochMillis: Long,
    val syncStatus: String = "PENDING"
)
