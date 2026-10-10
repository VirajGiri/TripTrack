package com.virajgiri.triptrack.data

import com.virajgiri.triptrack.data.local.ClientDao
import com.virajgiri.triptrack.data.local.ClientEntity
import com.virajgiri.triptrack.data.local.TripDao
import com.virajgiri.triptrack.data.local.TripEntity

class TripRepository(
    private val dao: TripDao,
    private val clientDao: ClientDao
) {
    val trips = dao.observeAll()
    val tripCount = dao.observeCount()
    val totalKm = dao.observeTotalKm()
    val totalDurationMinutes = dao.observeTotalDurationMinutes()
    val clients = clientDao.observeAll()

    suspend fun save(trip: TripEntity) = dao.insert(trip)
    suspend fun saveClient(client: ClientEntity) = clientDao.upsert(client)
    suspend fun deleteClient(id: String) = clientDao.deleteById(id)
    suspend fun getClient(id: String): ClientEntity? = clientDao.findById(id)
}
