package com.virajgiri.triptrack.data

import com.virajgiri.triptrack.data.local.BillEntity
import com.virajgiri.triptrack.data.local.TripEntity
import com.virajgiri.triptrack.domain.billing.BillCalculation
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object InvoiceSnapshotFactory {
    fun create(trip: TripEntity, calculation: BillCalculation): BillEntity {
        val items = JSONArray()
        calculation.lines.forEach { line ->
            items.put(JSONObject().put("label", line.label).put("amountPaise", line.amountPaise))
        }
        if (trip.tollPaise > 0L) {
            items.put(JSONObject().put("label", "Toll expense").put("amountPaise", trip.tollPaise))
        }
        if (trip.otherExpensePaise > 0L) {
            items.put(JSONObject().put("label", "Other expenses").put("amountPaise", trip.otherExpensePaise))
        }
        val total = Math.addExact(Math.addExact(calculation.totalPaise, trip.tollPaise), trip.otherExpensePaise)
        return BillEntity(
            id = UUID.randomUUID().toString(),
            tripId = trip.id,
            clientNameSnapshot = trip.clientName,
            tripDateSnapshot = trip.tripDate,
            packageTypeSnapshot = trip.packageType,
            vehicleNumberSnapshot = trip.vehicleNumber,
            distanceKmSnapshot = trip.distanceKm,
            durationMinutesSnapshot = trip.durationMinutes,
            lineItemsJson = items.toString(),
            totalPaise = total,
            createdAtEpochMillis = System.currentTimeMillis()
        )
    }
}
