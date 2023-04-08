package nl.tudelft.trustchain.offlinedigitaleuro.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Annotates class to be a Room Database with a table (entity) of the Word class


//@Database(entities = [UserData::class, Transactions::class, Token::class], version = 1)
//abstract class OfflineMoneyRoomDatabase : RoomDatabase() {
//    abstract fun userDao(): UserDao
//    abstract fun transactionsDao(): TransactionsDao
//    abstract fun tokensDao(): TokensDao
//}
@Database(entities = [UserData::class, Transactions::class, Token::class], version = 1)
abstract class OfflineMoneyRoomDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionsDao(): TransactionsDao
    abstract fun tokensDao(): TokensDao

    companion object {
        @Volatile
        private var INSTANCE: OfflineMoneyRoomDatabase? = null
        fun getDatabase(context: Context): OfflineMoneyRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OfflineMoneyRoomDatabase::class.java,
                    "item_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }

}
