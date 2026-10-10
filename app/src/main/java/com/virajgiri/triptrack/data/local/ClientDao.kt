package com.virajgiri.triptrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(client: ClientEntity)

    @Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ClientEntity?

    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun deleteById(id: String)
}
