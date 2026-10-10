package com.virajgiri.triptrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(trip: TripEntity)

    @Update
    suspend fun update(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :id AND ownerUid = :ownerUid")
    suspend fun deleteById(id: String, ownerUid: String)

    @Query("SELECT * FROM trips WHERE ownerUid = :ownerUid ORDER BY tripDate DESC, createdAtEpochMillis DESC")
    fun observeAll(ownerUid: String): Flow<List<TripEntity>>

    @Query("SELECT COUNT(*) FROM trips WHERE ownerUid = :ownerUid")
    fun observeCount(ownerUid: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(distanceKm), 0) FROM trips WHERE ownerUid = :ownerUid")
    fun observeTotalKm(ownerUid: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM trips WHERE ownerUid = :ownerUid")
    fun observeTotalDurationMinutes(ownerUid: String): Flow<Long>

    @Query("SELECT * FROM trips WHERE ownerUid = :ownerUid AND syncStatus != 'SYNCED'")
    suspend fun pendingForSync(ownerUid: String): List<TripEntity>

    @Query("UPDATE trips SET syncStatus = 'SYNCED' WHERE id = :id AND ownerUid = :ownerUid")
    suspend fun markSynced(id: String, ownerUid: String)

    @Query("UPDATE trips SET ownerUid = :ownerUid, syncStatus = 'PENDING' WHERE ownerUid = 'local'")
    suspend fun claimLocalData(ownerUid: String): Int
}
