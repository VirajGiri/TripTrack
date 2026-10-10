package com.virajgiri.triptrack.domain.billing

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class ValidatedTrip(
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val startKm: Double,
    val endKm: Double,
    val durationMinutes: Long,
    val distanceKm: Double
)

object TripInputValidator {
    private val timeFormatter = DateTimeFormatter.ofPattern("H:mm")

    fun validate(dateText: String, startText: String, endText: String, startKmText: String, endKmText: String): Result<ValidatedTrip> = runCatching {
        val date = LocalDate.parse(dateText.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
        val start = LocalTime.parse(startText.trim(), timeFormatter)
        val end = LocalTime.parse(endText.trim(), timeFormatter)
        val startKm = startKmText.trim().toDouble()
        val endKm = endKmText.trim().toDouble()
        require(startKm >= 0 && endKm >= 0) { "Odometer readings cannot be negative." }
        require(endKm >= startKm) { "End odometer must be greater than or equal to start odometer." }
        val startDateTime = date.atTime(start)
        var endDateTime = date.atTime(end)
        if (endDateTime.isBefore(startDateTime)) endDateTime = endDateTime.plusDays(1)
        val duration = Duration.between(startDateTime, endDateTime).toMinutes()
        require(duration > 0) { "Duty duration must be greater than zero." }
        ValidatedTrip(date, start, end, startKm, endKm, duration, endKm - startKm)
    }.recoverCatching { error ->
        when (error) {
            is DateTimeParseException -> throw IllegalArgumentException("Enter date as YYYY-MM-DD and times as HH:MM.")
            is NumberFormatException -> throw IllegalArgumentException("Enter valid numeric odometer readings.")
            else -> throw error
        }
    }
}
