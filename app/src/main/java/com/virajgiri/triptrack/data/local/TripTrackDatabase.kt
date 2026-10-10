package com.virajgiri.triptrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TripEntity::class, ClientEntity::class, BillEntity::class],
    version = 3,
    exportSchema = false
)
abstract class TripTrackDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun clientDao(): ClientDao
    abstract fun billDao(): BillDao

    companion object {
        @Volatile private var instance: TripTrackDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS clients (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        contact TEXT NOT NULL,
                        pickupBasePaise INTEGER,
                        pickupExtraHourPaise INTEGER,
                        pickupExtraKmPaise INTEGER,
                        localBasePaise INTEGER,
                        localExtraHourPaise INTEGER,
                        localExtraKmPaise INTEGER,
                        outstationDailyPaise INTEGER,
                        outstationExtraKmPaise INTEGER,
                        updatedAtEpochMillis INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS bills (
                        id TEXT NOT NULL PRIMARY KEY,
                        tripId TEXT NOT NULL,
                        clientNameSnapshot TEXT NOT NULL,
                        tripDateSnapshot TEXT NOT NULL,
                        packageTypeSnapshot TEXT NOT NULL,
                        vehicleNumberSnapshot TEXT NOT NULL,
                        distanceKmSnapshot REAL NOT NULL,
                        durationMinutesSnapshot INTEGER NOT NULL,
                        lineItemsJson TEXT NOT NULL,
                        totalPaise INTEGER NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL,
                        syncStatus TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bills_tripId ON bills (tripId)")
            }
        }

        fun getInstance(context: Context): TripTrackDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TripTrackDatabase::class.java,
                "triptrack.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }
    }
}
