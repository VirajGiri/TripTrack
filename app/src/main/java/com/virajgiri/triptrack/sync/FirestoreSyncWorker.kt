package com.virajgiri.triptrack.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.virajgiri.triptrack.data.local.TripTrackDatabase
import java.util.concurrent.TimeUnit

/**
 * Idempotent upload of pending records to the authenticated user's namespace.
 * It intentionally refuses to run for another UID and never uploads "local" records.
 */
class FirestoreSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val requestedUid = inputData.getString(KEY_UID) ?: return Result.failure()
        if (requestedUid == "local" || FirebaseApp.getApps(applicationContext).isEmpty()) {
            return Result.failure()
        }
        val user = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
            ?: return Result.retry()
        if (user.uid != requestedUid) return Result.failure()

        return try {
            val database = TripTrackDatabase.getInstance(applicationContext)
            val firestore = FirebaseFirestore.getInstance()
            val userCollection = firestore.collection("users").document(requestedUid)

            database.tripDao().pendingForSync(requestedUid).forEach { trip ->
                val payload = mapOf(
                    "id" to trip.id, "ownerUid" to trip.ownerUid,
                    "tripDate" to trip.tripDate, "clientName" to trip.clientName,
                    "packageType" to trip.packageType, "guestName" to trip.guestName,
                    "vehicleNumber" to trip.vehicleNumber, "startTime" to trip.startTime,
                    "endTime" to trip.endTime, "startOdometerKm" to trip.startOdometerKm,
                    "endOdometerKm" to trip.endOdometerKm, "tollPaise" to trip.tollPaise,
                    "otherExpensePaise" to trip.otherExpensePaise, "distanceKm" to trip.distanceKm,
                    "durationMinutes" to trip.durationMinutes,
                    "createdAtEpochMillis" to trip.createdAtEpochMillis
                )
                Tasks.await(userCollection.collection("trips").document(trip.id).set(payload))
                database.tripDao().markSynced(trip.id, requestedUid)
            }

            database.clientDao().pendingForSync(requestedUid).forEach { client ->
                val payload = mapOf(
                    "id" to client.id, "ownerUid" to client.ownerUid,
                    "name" to client.name, "contact" to client.contact,
                    "pickupBasePaise" to client.pickupBasePaise,
                    "pickupExtraHourPaise" to client.pickupExtraHourPaise,
                    "pickupExtraKmPaise" to client.pickupExtraKmPaise,
                    "localBasePaise" to client.localBasePaise,
                    "localExtraHourPaise" to client.localExtraHourPaise,
                    "localExtraKmPaise" to client.localExtraKmPaise,
                    "outstationDailyPaise" to client.outstationDailyPaise,
                    "outstationExtraKmPaise" to client.outstationExtraKmPaise,
                    "updatedAtEpochMillis" to client.updatedAtEpochMillis
                )
                Tasks.await(userCollection.collection("clients").document(client.id).set(payload))
                database.clientDao().markSynced(client.id, requestedUid)
            }

            database.billDao().pendingForSync(requestedUid).forEach { bill ->
                val payload = mapOf(
                    "id" to bill.id, "ownerUid" to bill.ownerUid,
                    "tripId" to bill.tripId, "clientNameSnapshot" to bill.clientNameSnapshot,
                    "tripDateSnapshot" to bill.tripDateSnapshot,
                    "packageTypeSnapshot" to bill.packageTypeSnapshot,
                    "vehicleNumberSnapshot" to bill.vehicleNumberSnapshot,
                    "distanceKmSnapshot" to bill.distanceKmSnapshot,
                    "durationMinutesSnapshot" to bill.durationMinutesSnapshot,
                    "lineItemsJson" to bill.lineItemsJson, "totalPaise" to bill.totalPaise,
                    "createdAtEpochMillis" to bill.createdAtEpochMillis
                )
                Tasks.await(userCollection.collection("bills").document(bill.id).set(payload))
                database.billDao().markSynced(bill.id, requestedUid)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val KEY_UID = "owner_uid"

        fun enqueue(context: Context, uid: String) {
            if (uid.isBlank() || uid == "local") return
            val request = OneTimeWorkRequestBuilder<FirestoreSyncWorker>()
                .setInputData(workDataOf(KEY_UID to uid))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "triptrack-sync-$uid",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
