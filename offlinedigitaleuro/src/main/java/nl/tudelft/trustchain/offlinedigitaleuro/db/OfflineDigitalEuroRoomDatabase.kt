package nl.tudelft.trustchain.offlinedigitaleuro.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Annotates class to be a Room Database with a table (entity) of the Word class

@Database(entities = [UserData::class, Transactions::class, Token::class, WebOfTrust::class], version = 1, exportSchema = false)
abstract class OfflineDigitalEuroRoomDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionsDao(): TransactionsDao
    abstract fun tokensDao(): TokensDao
    abstract fun webOfTrustDao(): WebOfTrustDAO

    companion object {
        @Volatile
        private var INSTANCE: OfflineDigitalEuroRoomDatabase? = null
        fun getDatabase(context: Context): OfflineDigitalEuroRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OfflineDigitalEuroRoomDatabase::class.java,
                    "item_database"
                )
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }

}
