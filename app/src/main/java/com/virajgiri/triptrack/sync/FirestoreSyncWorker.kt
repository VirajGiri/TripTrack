package com.virajgiri.triptrack.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.virajgiri.triptrack.data.local.BillEntity
import com.virajgiri.triptrack.data.local.ClientEntity
import com.virajgiri.triptrack.data.local.TripEntity
import com.virajgiri.triptrack.data.local.TripTrackDatabase
import java.util.concurrent.TimeUnit

/**
 * Restores missing cloud records and uploads pending local records for the authenticated UID.
 * Existing local rows are not overwritten during restore, avoiding silent loss of local edits.
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

            // Pull only records not already present locally. Conflicting rows are left untouched.
            Tasks.await(userCollection.collection("trips").get()).documents.forEach { doc ->
                if (database.tripDao().findById(doc.id, requestedUid) == null) {
                    database.tripDao().upsert(
                        TripEntity(
                            id = doc.id,
                            tripDate = doc.getString("tripDate") ?: "",
                            clientName = doc.getString("clientName") ?: "",
                            packageType = doc.getString("packageType") ?: "",
                            guestName = doc.getString("guestName") ?: "",
                            vehicleNumber = doc.getString("vehicleNumber") ?: "",
                            startTime = doc.getString("startTime") ?: "",
                            endTime = doc.getString("endTime") ?: "",
                            startOdometerKm = doc.getDouble("startOdometerKm") ?: 0.0,
                            endOdometerKm = doc.getDouble("endOdometerKm") ?: 0.0,
                            tollPaise = doc.getLong("tollPaise") ?: 0L,
                            otherExpensePaise = doc.getLong("otherExpensePaise") ?: 0L,
                            distanceKm = doc.getDouble("distanceKm") ?: 0.0,
                            durationMinutes = doc.getLong("durationMinutes") ?: 0L,
                            createdAtEpochMillis = doc.getLong("createdAtEpochMillis") ?: 0L,
                            syncStatus = "SYNCED",
                            ownerUid = requestedUid
                        )
                    )
                }
            }

            Tasks.await(userCollection.collection("clients").get()).documents.forEach { doc ->
                if (database.clientDao().findById(doc.id, requestedUid) == null) {
                    database.clientDao().upsert(
                        ClientEntity(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            contact = doc.getString("contact") ?: "",
                            pickupBasePaise = doc.getLong("pickupBasePaise"),
                            pickupExtraHourPaise = doc.getLong("pickupExtraHourPaise"),
                            pickupExtraKmPaise = doc.getLong("pickupExtraKmPaise"),
                            localBasePaise = doc.getLong("localBasePaise"),
                            localExtraHourPaise = doc.getLong("localExtraHourPaise"),
                            localExtraKmPaise = doc.getLong("localExtraKmPaise"),
                            outstationDailyPaise = doc.getLong("outstationDailyPaise"),
                            outstationExtraKmPaise = doc.getLong("outstationExtraKmPaise"),
                            updatedAtEpochMillis = doc.getLong("updatedAtEpochMillis") ?: 0L,
                            syncStatus = "SYNCED",
                            ownerUid = requestedUid
                        )
                    )
                }
            }

            Tasks.await(userCollection.collection("bills").get()).documents.forEach { doc ->
                if (database.billDao().findById(doc.id, requestedUid) == null) {
                    database.billDao().upsert(
                        BillEntity(
                            id = doc.id,
                            tripId = doc.getString("tripId") ?: "",
                            clientNameSnapshot = doc.getString("clientNameSnapshot") ?: "",
                            tripDateSnapshot = doc.getString("tripDateSnapshot") ?: "",
                            packageTypeSnapshot = doc.getString("packageTypeSnapshot") ?: "",
                            vehicleNumberSnapshot = doc.getString("vehicleNumberSnapshot") ?: "",
                            distanceKmSnapshot = doc.getDouble("distanceKmSnapshot") ?: 0.0,
                            durationMinutesSnapshot = doc.getLong("durationMinutesSnapshot") ?: 0L,
                            lineItemsJson = doc.getString("lineItemsJson") ?: "[]",
                            totalPaise = doc.getLong("totalPaise") ?: 0L,
                            createdAtEpochMillis = doc.getLong("createdAtEpochMillis") ?: 0L,
                            syncStatus = "SYNCED",
                            ownerUid = requestedUid
                        )
                    )
                }
            }

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
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
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
