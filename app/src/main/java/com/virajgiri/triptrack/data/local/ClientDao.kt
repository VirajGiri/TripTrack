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

    @Query("SELECT * FROM clients WHERE ownerUid = :ownerUid AND syncStatus NOT LIKE 'DELETED%' ORDER BY name COLLATE NOCASE")
    fun observeAll(ownerUid: String): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id AND ownerUid = :ownerUid LIMIT 1")
    suspend fun findById(id: String, ownerUid: String): ClientEntity?

    @Query("UPDATE clients SET syncStatus = 'DELETED' WHERE id = :id AND ownerUid = :ownerUid")
    suspend fun deleteById(id: String, ownerUid: String)

    @Query("SELECT * FROM clients WHERE ownerUid = :ownerUid AND syncStatus NOT IN ('SYNCED', 'DELETED_SYNCED')")
    suspend fun pendingForSync(ownerUid: String): List<ClientEntity>

    @Query("UPDATE clients SET syncStatus = CASE WHEN syncStatus LIKE 'DELETED%' THEN 'DELETED_SYNCED' ELSE 'SYNCED' END WHERE id = :id AND ownerUid = :ownerUid")
    suspend fun markSynced(id: String, ownerUid: String)

    @Query("UPDATE clients SET syncStatus = 'DELETED_SYNCED' WHERE id = :id AND ownerUid = :ownerUid")
    suspend fun markDeletedFromCloud(id: String, ownerUid: String)

    @Query("UPDATE clients SET ownerUid = :ownerUid, syncStatus = CASE WHEN syncStatus LIKE 'DELETED%' THEN 'DELETED' ELSE 'PENDING' END WHERE ownerUid = 'local'")
    suspend fun claimLocalData(ownerUid: String): Int
}
