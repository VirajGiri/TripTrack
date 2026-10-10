package com.virajgiri.triptrack.data

import com.virajgiri.triptrack.data.local.TripDao
import com.virajgiri.triptrack.data.local.TripEntity

class TripRepository(private val dao: TripDao) {
    val trips = dao.observeAll()
    val tripCount = dao.observeCount()
    val totalKm = dao.observeTotalKm()
    val totalDurationMinutes = dao.observeTotalDurationMinutes()
    suspend fun save(trip: TripEntity) = dao.insert(trip)
}
