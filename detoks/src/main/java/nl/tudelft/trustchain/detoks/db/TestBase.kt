package nl.tudelft.trustchain.detoks.db
import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import nl.tudelft.detoks.sqldelight.Database

class TestBase (context: Context){
    private val driver = AndroidSqliteDriver(Database.Schema, context, "testbase.db")
    private val database = Database(driver)

    private lateinit var instance: TestBase
    fun getInstance(context: Context): TestBase {
        if (!::instance.isInitialized) {
            instance = TestBase(context)
        }
        return instance
    }

//    fun getTransaction(transactionID: Int){
//        database.
//    }
}



