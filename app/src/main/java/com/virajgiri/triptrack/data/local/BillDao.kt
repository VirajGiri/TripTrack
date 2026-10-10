package com.virajgiri.triptrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(bill: BillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bill: BillEntity)

    @Query("SELECT * FROM bills WHERE ownerUid = :ownerUid ORDER BY createdAtEpochMillis DESC")
    fun observeAll(ownerUid: String): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :id AND ownerUid = :ownerUid LIMIT 1")
    suspend fun findById(id: String, ownerUid: String): BillEntity?

    @Query("SELECT * FROM bills WHERE ownerUid = :ownerUid AND syncStatus != 'SYNCED'")
    suspend fun pendingForSync(ownerUid: String): List<BillEntity>

    @Query("UPDATE bills SET syncStatus = 'SYNCED' WHERE id = :id AND ownerUid = :ownerUid")
    suspend fun markSynced(id: String, ownerUid: String)

    @Query("UPDATE bills SET ownerUid = :ownerUid, syncStatus = 'PENDING' WHERE ownerUid = 'local'")
    suspend fun claimLocalData(ownerUid: String): Int
}
