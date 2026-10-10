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

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM trips ORDER BY tripDate DESC, createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<TripEntity>>

    @Query("SELECT COUNT(*) FROM trips")
    fun observeCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(distanceKm), 0) FROM trips")
    fun observeTotalKm(): Flow<Double>

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM trips")
    fun observeTotalDurationMinutes(): Flow<Long>
}
