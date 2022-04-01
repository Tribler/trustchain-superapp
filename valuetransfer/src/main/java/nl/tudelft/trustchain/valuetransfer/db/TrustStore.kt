package nl.tudelft.trustchain.valuetransfer.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import nl.tudelft.trustchain.valuetransfer.entity.TrustScore

@Database(entities = [TrustScore::class], version = 2)
@TypeConverters(Converters::class)
abstract class TrustStore : RoomDatabase() {
    abstract fun trustDao(): TrustDao

    companion object {
        private var instance: TrustStore? = null

        fun getInstance(context: Context): TrustStore {
            return instance ?: Room.databaseBuilder(
                context,
                TrustStore::class.java,
                "truststore"
            ).build().also { instance = it }
        }
    }
}
