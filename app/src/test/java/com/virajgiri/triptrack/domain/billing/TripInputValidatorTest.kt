package com.virajgiri.triptrack.domain.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripInputValidatorTest {
    @Test
    fun validatesDistanceAndOvernightDuration() {
        val result = TripInputValidator.validate("2026-10-10", "23:00", "01:30", "100.0", "145.5")
        assertTrue(result.isSuccess)
        val trip = result.getOrThrow()
        assertEquals(150L, trip.durationMinutes)
        assertEquals(45.5, trip.distanceKm, 0.0001)
    }

    @Test
    fun rejectsEndOdometerBelowStart() {
        val result = TripInputValidator.validate("2026-10-10", "09:00", "10:00", "100", "99")
        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsZeroDuration() {
        val result = TripInputValidator.validate("2026-10-10", "09:00", "09:00", "100", "100")
        assertTrue(result.isFailure)
    }
}
