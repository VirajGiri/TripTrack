package com.virajgiri.triptrack.data

import com.virajgiri.triptrack.data.local.BillDao
import com.virajgiri.triptrack.data.local.BillEntity
import com.virajgiri.triptrack.data.local.ClientDao
import com.virajgiri.triptrack.data.local.ClientEntity
import com.virajgiri.triptrack.data.local.TripDao
import com.virajgiri.triptrack.data.local.TripEntity

class TripRepository(
    private val dao: TripDao,
    private val clientDao: ClientDao,
    private val billDao: BillDao
) {
    val trips = dao.observeAll()
    val tripCount = dao.observeCount()
    val totalKm = dao.observeTotalKm()
    val totalDurationMinutes = dao.observeTotalDurationMinutes()
    val clients = clientDao.observeAll()
    val bills = billDao.observeAll()

    suspend fun save(trip: TripEntity) = dao.insert(trip)
    suspend fun updateTrip(trip: TripEntity) = dao.update(trip)
    suspend fun deleteTrip(id: String) = dao.deleteById(id)
    suspend fun saveClient(client: ClientEntity) = clientDao.upsert(client)
    suspend fun deleteClient(id: String) = clientDao.deleteById(id)
    suspend fun getClient(id: String): ClientEntity? = clientDao.findById(id)
    suspend fun saveBill(bill: BillEntity) = billDao.insert(bill)
    suspend fun getBill(id: String): BillEntity? = billDao.findById(id)
}
