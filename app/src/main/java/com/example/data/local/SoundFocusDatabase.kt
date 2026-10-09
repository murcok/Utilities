package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.AudioProfile
import com.example.data.model.FocusLog
import com.example.data.model.MonitoredApp

@Database(
    entities = [MonitoredApp::class, FocusLog::class, AudioProfile::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SoundFocusDatabase : RoomDatabase() {

    abstract fun soundFocusDao(): SoundFocusDao

    companion object {
        @Volatile
        private var INSTANCE: SoundFocusDatabase? = null

        fun getInstance(context: Context): SoundFocusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SoundFocusDatabase::class.java,
                    "sound_focus_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
