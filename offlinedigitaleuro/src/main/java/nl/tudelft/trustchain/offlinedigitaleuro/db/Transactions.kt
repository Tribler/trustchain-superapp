package nl.tudelft.trustchain.offlinedigitaleuro.db

import androidx.room.*


@Entity(tableName = "transactions_table")
class Transactions(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "transaction_datetime") var transaction_datetime: String,
    @ColumnInfo(name = "pubk_sender") var pubk_sender: String,
    @ColumnInfo(name = "pubk_receiver") var pubk_receiver: String,
    @ColumnInfo(name = "amount") var amount: Double,
    @ColumnInfo(name = "verified") var verified: Boolean,
)


@Dao
interface TransactionsDao {
    // Get all transactions from database
    @Query("SELECT * FROM transactions_table")
    fun getTransactionData(): List<Transactions>

    // Get limited amount of transactions from database
    @Query("SELECT * FROM transactions_table ORDER BY id DESC LIMIT :limit_value")
    fun getLimitedTransactionsData(limit_value: Int): List<Transactions>

    // Insert transactions into the database
    @Insert
    suspend fun insertTransaction(transactions: Transactions)

    // Delete transactions
    @Delete
    suspend fun deleteTransactionData(transactions: Transactions)

//    @Query("DELETE FROM transactions_table")
//    suspend fun deleteTransactionData()

}
