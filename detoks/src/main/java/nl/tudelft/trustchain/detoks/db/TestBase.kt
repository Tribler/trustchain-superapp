package nl.tudelft.trustchain.detoks.db
import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import nl.tudelft.detoks.sqldelight.Database

class TestBase(context: Context){
    private val driver = AndroidSqliteDriver(Database.Schema, context, "testbase.db")
    private val database = Database(driver)

    private lateinit var instance: TestBase
    fun getInstance(context: Context): TestBase {
        if (!::instance.isInitialized) {
            instance = TestBase(context)
        }
        return instance
    }

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
    fun getAllTransactions() = database.testbaseQueries.selectAll().executeAsList()

    /**
     * Adds a transaction to the database
     */
    fun addTransaction(transactionID: Int, sendFrom: String, sendTo: String, amount: Int){
        database.testbaseQueries.addTest(transactionID.toLong(), sendFrom, sendTo, amount.toLong())
    }

    /**
     * Deletes a transaction from the database
     */
    fun deleteTransaction(transactionID: Int){
        database.testbaseQueries.deleteTest(transactionID.toLong())
    }


    /**
     * Deletes all transactions from the database
     */
    fun deleteAll() {
        database.testbaseQueries.deleteAll()
    }
}



