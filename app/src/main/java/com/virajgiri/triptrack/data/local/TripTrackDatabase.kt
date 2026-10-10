package com.virajgiri.triptrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TripEntity::class], version = 1, exportSchema = false)
abstract class TripTrackDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile private var instance: TripTrackDatabase? = null

        fun getInstance(context: Context): TripTrackDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TripTrackDatabase::class.java,
                "triptrack.db"
            ).build().also { instance = it }
        }
    }
}
