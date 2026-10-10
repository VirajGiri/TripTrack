package com.virajgiri.triptrack.domain.billing

import com.virajgiri.triptrack.data.local.ClientEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingCalculatorTest {
    private fun client(
        localBase: Long? = 150_000L,
        localHour: Long? = 12_000L,
        localKm: Long? = 500L,
        pickupBase: Long? = 80_000L,
        pickupHour: Long? = 10_000L,
        pickupKm: Long? = 400L,
        outstationBase: Long? = 200_000L,
        outstationKm: Long? = 1_200L
    ) = ClientEntity(
        id = "client-1",
        name = "Test client",
        localBasePaise = localBase,
        localExtraHourPaise = localHour,
        localExtraKmPaise = localKm,
        pickupBasePaise = pickupBase,
        pickupExtraHourPaise = pickupHour,
        pickupExtraKmPaise = pickupKm,
        outstationDailyPaise = outstationBase,
        outstationExtraKmPaise = outstationKm,
        updatedAtEpochMillis = 1L
    )

    @Test
    fun localAtAllowanceHasBaseOnly() {
        val result = BillingCalculator.calculate(client(), TripPackage.LOCAL, 80.0, 480L)
        assertTrue(result is BillingResult.Success)
        assertEquals(150_000L, (result as BillingResult.Success).bill.totalPaise)
    }

    @Test
    fun localExcessTimeAndDistanceAreBothItemized() {
        val result = BillingCalculator.calculate(client(), TripPackage.LOCAL, 90.0, 540L)
        assertTrue(result is BillingResult.Success)
        val bill = (result as BillingResult.Success).bill
        assertEquals(167_000L, bill.totalPaise)
        assertEquals(3, bill.lines.size)
    }

    @Test
    fun missingRequiredExcessRateBlocksBill() {
        val result = BillingCalculator.calculate(client(localKm = null), TripPackage.LOCAL, 81.0, 480L)
        assertTrue(result is BillingResult.MissingRate)
    }

    @Test
    fun outstationExtraDistanceUsesTwelveRupeesPerKm() {
        val result = BillingCalculator.calculate(client(), TripPackage.OUTSTATION, 310.0, 600L)
        assertTrue(result is BillingResult.Success)
        assertEquals(212_000L, (result as BillingResult.Success).bill.totalPaise)
    }

    @Test
    fun multiDayOutstationRequiresDayWiseEntries() {
        val result = BillingCalculator.calculate(client(), TripPackage.OUTSTATION, 310.0, 1_500L)
        assertTrue(result is BillingResult.UnsupportedTrip)
    }
}
