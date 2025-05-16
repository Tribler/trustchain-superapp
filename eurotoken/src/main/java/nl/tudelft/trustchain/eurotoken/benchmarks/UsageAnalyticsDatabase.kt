package nl.tudelft.trustchain.eurotoken.benchmarks

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TransactionStartEvent::class,
        TransactionErrorEvent::class,
        TransactionDoneEvent::class,
        TransactionCancelEvent::class,
        TransferStartEvent::class,
        TransferErrorEvent::class,
        TransferDoneEvent::class
    ],
    version = 1, // Increment if schema changes.
    exportSchema = false
)
abstract class UsageAnalyticsDatabase : RoomDatabase() {

    abstract fun usageEventsDao(): UsageEventsDao

    companion object {
        @Volatile
        private var INSTANCE: UsageAnalyticsDatabase? = null

        fun getInstance(context: Context): UsageAnalyticsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UsageAnalyticsDatabase::class.java,
                    "eurotoken_usage_analytics_db"
                )
                    // Add migrations here if you change schema later
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
