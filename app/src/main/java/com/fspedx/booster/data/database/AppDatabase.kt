package com.fspedx.booster.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fspedx.booster.data.models.LagSpike
import com.fspedx.booster.data.models.TouchEvent
import com.fspedx.booster.utils.constants.Constants

/**
 * Room Database singleton.
 * Holds all tables: touch_events and lag_spikes.
 */
@Database(
    entities = [TouchEvent::class, LagSpike::class],
    version = Constants.DB_VERSION,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun touchEventDao(): TouchEventDao
    abstract fun lagSpikeDao(): LagSpikeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * For testing: clear the instance.
         */
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
