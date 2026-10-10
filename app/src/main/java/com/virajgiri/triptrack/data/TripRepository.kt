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
    private val billDao: BillDao,
    val ownerUid: String = "local"
) {
    val trips = dao.observeAll(ownerUid)
    val tripCount = dao.observeCount(ownerUid)
    val totalKm = dao.observeTotalKm(ownerUid)
    val totalDurationMinutes = dao.observeTotalDurationMinutes(ownerUid)
    val clients = clientDao.observeAll(ownerUid)
    val bills = billDao.observeAll(ownerUid)

    suspend fun save(trip: TripEntity) = dao.insert(trip.copy(ownerUid = ownerUid, syncStatus = "PENDING"))
    suspend fun updateTrip(trip: TripEntity) = dao.update(trip.copy(ownerUid = ownerUid, syncStatus = "PENDING"))
    suspend fun deleteTrip(id: String) = dao.deleteById(id, ownerUid)
    suspend fun saveClient(client: ClientEntity) = clientDao.upsert(client.copy(ownerUid = ownerUid, syncStatus = "PENDING"))
    suspend fun deleteClient(id: String) = clientDao.deleteById(id, ownerUid)
    suspend fun getClient(id: String): ClientEntity? = clientDao.findById(id, ownerUid)
    suspend fun saveBill(bill: BillEntity) = billDao.insert(bill.copy(ownerUid = ownerUid, syncStatus = "PENDING"))
    suspend fun getBill(id: String): BillEntity? = billDao.findById(id, ownerUid)

    /** Explicit opt-in: move records created before sign-in into the currently signed-in account. */
    suspend fun claimLocalDataForThisAccount(): Int {
        require(ownerUid != "local") { "Sign in before importing local records." }
        return dao.claimLocalData(ownerUid) +
            clientDao.claimLocalData(ownerUid) +
            billDao.claimLocalData(ownerUid)
    }
}
