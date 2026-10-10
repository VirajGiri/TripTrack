package com.virajgiri.triptrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TripEntity::class, ClientEntity::class],
    version = 2,
    exportSchema = false
)
abstract class TripTrackDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun clientDao(): ClientDao

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

        fun getInstance(context: Context): TripTrackDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TripTrackDatabase::class.java,
                "triptrack.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
