package com.virajgiri.triptrack.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * All money values are stored in paise. Null means the rate has not been configured;
 * zero is a deliberate configured value.
 */
@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: String,
    val name: String,
    val contact: String = "",
    val pickupBasePaise: Long? = null,
    val pickupExtraHourPaise: Long? = null,
    val pickupExtraKmPaise: Long? = null,
    val localBasePaise: Long? = null,
    val localExtraHourPaise: Long? = null,
    val localExtraKmPaise: Long? = null,
    val outstationDailyPaise: Long? = null,
    val outstationExtraKmPaise: Long? = 1200L,
    val updatedAtEpochMillis: Long,
    @ColumnInfo(defaultValue = "'PENDING'") val syncStatus: String = "PENDING",
    @ColumnInfo(defaultValue = "'local'") val ownerUid: String = "local"
)
