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

    @Query("SELECT * FROM bills ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): BillEntity?
}
