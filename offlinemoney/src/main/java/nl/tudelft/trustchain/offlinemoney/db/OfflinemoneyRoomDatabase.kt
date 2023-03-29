package nl.tudelft.trustchain.offlinemoney.db

import androidx.room.Database
import androidx.room.RoomDatabase

// Annotates class to be a Room Database with a table (entity) of the Word class


@Database(entities = [UserData::class, Transactions::class], version = 1)
abstract class OfflineMoneyRoomDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionsDao(): TransactionsDao
}
