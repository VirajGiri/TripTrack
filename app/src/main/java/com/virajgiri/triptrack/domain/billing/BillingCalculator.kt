package com.virajgiri.triptrack.domain.billing

import com.virajgiri.triptrack.data.local.ClientEntity
import java.math.BigDecimal
import java.math.RoundingMode

enum class TripPackage { PICKUP_DROP, LOCAL, OUTSTATION }

data class BillLine(val label: String, val amountPaise: Long)
data class BillCalculation(
    val lines: List<BillLine>,
    val totalPaise: Long,
    val distanceKm: Double,
    val durationMinutes: Long
)

sealed class BillingResult {
    data class Success(val bill: BillCalculation) : BillingResult()
    data class MissingRate(val rateName: String) : BillingResult()
    data class UnsupportedTrip(val reason: String) : BillingResult()
}

/**
 * Rates are never guessed. Any missing rate needed by a bill returns MissingRate.
 * Excess time is prorated to the minute; amounts are rounded to the nearest paise.
 */
object BillingCalculator {
    fun calculate(
        client: ClientEntity,
        packageType: TripPackage,
        distanceKm: Double,
        durationMinutes: Long
    ): BillingResult {
        if (!distanceKm.isFinite() || distanceKm < 0.0) {
            return BillingResult.UnsupportedTrip("Distance must be a finite non-negative number.")
        }
        if (durationMinutes <= 0L) {
            return BillingResult.UnsupportedTrip("Duty duration must be greater than zero.")
        }

        val lines = mutableListOf<BillLine>()
        when (packageType) {
            TripPackage.PICKUP_DROP -> {
                val base = client.pickupBasePaise ?: return BillingResult.MissingRate("Pickup/drop base rate")
                lines += BillLine("Pickup/drop package", base)
                addExcessTime(lines, durationMinutes, 240L, client.pickupExtraHourPaise, "Pickup/drop extra time")
                    ?.let { return BillingResult.MissingRate(it) }
                addExcessDistance(lines, distanceKm, 40.0, client.pickupExtraKmPaise, "Pickup/drop extra distance")
                    ?.let { return BillingResult.MissingRate(it) }
            }
            TripPackage.LOCAL -> {
                val base = client.localBasePaise ?: return BillingResult.MissingRate("Local base rate")
                lines += BillLine("Local package", base)
                addExcessTime(lines, durationMinutes, 480L, client.localExtraHourPaise, "Local extra time")
                    ?.let { return BillingResult.MissingRate(it) }
                addExcessDistance(lines, distanceKm, 80.0, client.localExtraKmPaise, "Local extra distance")
                    ?.let { return BillingResult.MissingRate(it) }
            }
            TripPackage.OUTSTATION -> {
                if (durationMinutes > 24L * 60L) {
                    return BillingResult.UnsupportedTrip("Outstation trips longer than 24 hours need day-wise distance entries before billing.")
                }
                val base = client.outstationDailyPaise ?: return BillingResult.MissingRate("Outstation daily rate")
                lines += BillLine("Outstation daily package", base)
                addExcessDistance(lines, distanceKm, 300.0, client.outstationExtraKmPaise, "Outstation extra distance")
                    ?.let { return BillingResult.MissingRate(it) }
            }
        }

        val total = lines.fold(0L) { sum, line -> Math.addExact(sum, line.amountPaise) }
        return BillingResult.Success(BillCalculation(lines, total, distanceKm, durationMinutes))
    }

    private fun addExcessTime(
        lines: MutableList<BillLine>,
        durationMinutes: Long,
        allowanceMinutes: Long,
        hourlyRatePaise: Long?,
        label: String
    ): String? {
        val excessMinutes = (durationMinutes - allowanceMinutes).coerceAtLeast(0L)
        if (excessMinutes == 0L) return null
        val rate = hourlyRatePaise ?: return "$label rate"
        val amount = BigDecimal.valueOf(rate).multiply(BigDecimal.valueOf(excessMinutes))
            .divide(BigDecimal.valueOf(60L), 0, RoundingMode.HALF_UP).longValueExact()
        lines += BillLine("$label ($excessMinutes min)", amount)
        return null
    }

    private fun addExcessDistance(
        lines: MutableList<BillLine>,
        distanceKm: Double,
        allowanceKm: Double,
        perKmRatePaise: Long?,
        label: String
    ): String? {
        val excessKm = (distanceKm - allowanceKm).coerceAtLeast(0.0)
        if (excessKm <= 0.0000001) return null
        val rate = perKmRatePaise ?: return "$label rate"
        val amount = BigDecimal.valueOf(rate).multiply(BigDecimal.valueOf(excessKm))
            .setScale(0, RoundingMode.HALF_UP).longValueExact()
        lines += BillLine("$label (%.2f km)".format(excessKm), amount)
        return null
    }
}
