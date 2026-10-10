package com.virajgiri.triptrack.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val tripDate: String,
    val clientName: String,
    val packageType: String,
    val guestName: String,
    val vehicleNumber: String,
    val startTime: String,
    val endTime: String,
    val startOdometerKm: Double,
    val endOdometerKm: Double,
    val tollPaise: Long,
    val otherExpensePaise: Long,
    val distanceKm: Double,
    val durationMinutes: Long,
    val createdAtEpochMillis: Long,
    @ColumnInfo(defaultValue = "0") val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val syncStatus: String = "PENDING",
    @ColumnInfo(defaultValue = "'local'") val ownerUid: String = "local"
)
