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

    @Query("SELECT * FROM clients WHERE ownerUid = :ownerUid ORDER BY name COLLATE NOCASE")
    fun observeAll(ownerUid: String): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id AND ownerUid = :ownerUid LIMIT 1")
    suspend fun findById(id: String, ownerUid: String): ClientEntity?

    @Query("DELETE FROM clients WHERE id = :id AND ownerUid = :ownerUid")
    suspend fun deleteById(id: String, ownerUid: String)

    @Query("SELECT * FROM clients WHERE ownerUid = :ownerUid AND syncStatus != 'SYNCED'")
    suspend fun pendingForSync(ownerUid: String): List<ClientEntity>

    @Query("UPDATE clients SET syncStatus = 'SYNCED' WHERE id = :id AND ownerUid = :ownerUid")
    suspend fun markSynced(id: String, ownerUid: String)

    @Query("UPDATE clients SET ownerUid = :ownerUid, syncStatus = 'PENDING' WHERE ownerUid = 'local'")
    suspend fun claimLocalData(ownerUid: String): Int
}
