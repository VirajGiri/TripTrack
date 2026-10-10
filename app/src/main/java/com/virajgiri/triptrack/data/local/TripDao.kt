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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(trip: TripEntity)

    @Query("SELECT * FROM trips WHERE id = :id AND ownerUid = :ownerUid LIMIT 1")
    suspend fun findById(id: String, ownerUid: String): TripEntity?

    @Update
    suspend fun update(trip: TripEntity)

    @Query("UPDATE trips SET syncStatus = 'DELETED' WHERE id = :id AND ownerUid = :ownerUid")
    suspend fun deleteById(id: String, ownerUid: String)

    @Query("SELECT * FROM trips WHERE ownerUid = :ownerUid AND syncStatus NOT LIKE 'DELETED%' ORDER BY tripDate DESC, createdAtEpochMillis DESC")
    fun observeAll(ownerUid: String): Flow<List<TripEntity>>

    @Query("SELECT COUNT(*) FROM trips WHERE ownerUid = :ownerUid AND syncStatus NOT LIKE 'DELETED%'")
    fun observeCount(ownerUid: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(distanceKm), 0) FROM trips WHERE ownerUid = :ownerUid AND syncStatus NOT LIKE 'DELETED%'")
    fun observeTotalKm(ownerUid: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM trips WHERE ownerUid = :ownerUid AND syncStatus NOT LIKE 'DELETED%'")
    fun observeTotalDurationMinutes(ownerUid: String): Flow<Long>

    @Query("SELECT * FROM trips WHERE ownerUid = :ownerUid AND syncStatus NOT IN ('SYNCED', 'DELETED_SYNCED')")
    suspend fun pendingForSync(ownerUid: String): List<TripEntity>

    @Query("UPDATE trips SET syncStatus = CASE WHEN syncStatus LIKE 'DELETED%' THEN 'DELETED_SYNCED' ELSE 'SYNCED' END WHERE id = :id AND ownerUid = :ownerUid")
    suspend fun markSynced(id: String, ownerUid: String)

    @Query("UPDATE trips SET syncStatus = 'DELETED_SYNCED' WHERE id = :id AND ownerUid = :ownerUid")
    suspend fun markDeletedFromCloud(id: String, ownerUid: String)

    @Query("UPDATE trips SET ownerUid = :ownerUid, syncStatus = CASE WHEN syncStatus LIKE 'DELETED%' THEN 'DELETED' ELSE 'PENDING' END WHERE ownerUid = 'local'")
    suspend fun claimLocalData(ownerUid: String): Int
}
