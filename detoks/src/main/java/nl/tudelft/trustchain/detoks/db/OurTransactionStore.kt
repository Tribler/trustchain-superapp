package nl.tudelft.trustchain.detoks.db
import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import nl.tudelft.detoks.sqldelight.Database

class OurTransactionStore(context: Context){
    private val driver = AndroidSqliteDriver(Database.Schema, context, "ourtransactionstore.db")
    private val database = Database(driver)

//    /**
//     * Maps the keys and accompanying trust scores out of the database into a kotlin [Transaction] object.
//     */
//    private val messageMapper = {
//            transactionID : Long,
//            sendFrom : String,
//            sendTo : String,
//            amount : Long
//        ->
//        Transaction(
//            transactionID.toInt(),
//            sendFrom,
//            sendTo
//            amount.toInt()
//        )
//    }

    /**
     * Returns all transactions from the database
     */
    fun getAllTransactions() = database.ourTransactionStoreQueries.selectAll().executeAsList()

    fun getTransactionByPeer(peer: String) {
        database.ourTransactionStoreQueries.getTransactionByPeer(peer)
    }

    /**
     * Adds a transaction to the database
     */
    fun addTransaction(transactionID: Int, sendFrom: String, sendTo: String, type: String){
        database.ourTransactionStoreQueries.addTransaction(transactionID.toLong(), sendFrom, sendTo, type)
    }

    /**
     * Deletes a transaction from the database
     */
    fun deleteTransaction(transactionID: Int){
        database.ourTransactionStoreQueries.deleteTransactionByID(transactionID.toLong())
    }


    /**
     * Deletes all transactions from the database
     */
    fun deleteAll() {
        database.ourTransactionStoreQueries.deleteAll()
    }

    companion object {
        private lateinit var instance: OurTransactionStore
        fun getInstance(context: Context): OurTransactionStore {
            if (!::instance.isInitialized) {
                instance = OurTransactionStore(context)
            }
            return instance
        }
    }
}



