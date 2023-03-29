package nl.tudelft.trustchain.offlinemoney

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.offlinemoney.db.OfflineMoneyRoomDatabase
import nl.tudelft.trustchain.offlinemoney.db.UserDao
import nl.tudelft.trustchain.offlinemoney.db.UserData

class MainActivityOfflineMoney() : BaseActivity() {
    private lateinit var userDao: UserDao
    override val navigationGraph = R.navigation.nav_graph_offlinemoney
    override val bottomNavigationMenu = R.menu.offlinemoney_menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(
            applicationContext,
            OfflineMoneyRoomDatabase::class.java, "offline_money_db"
        ).build()

        userDao = db.userDao()

        testDB()

    }

    private fun testDB() {
        lifecycleScope.launch(Dispatchers.IO) {
//            //Insert
//            Log.i("MyTAG","*****     Inserting 1 User     **********")
//            userDao.insertUser(
//                UserData(0,"Java","Alex", "dfslafk", 0.023)
//            )
//            Log.i("MyTAG","*****     Inserted 1 User       **********")

            // Query
//            var users = userDao.getUserData()
//            Log.i("MyTAG","*****   ${users.size} users there *****")
//            var user = users[0]
//            Log.i("MyTAG","id: ${user.id} name: ${user.username} public_key: ${user.public_key} private_key ${user.private_key} balance: ${user.money_balance.toString()}")

//            //Update
//            user.money_balance = 50.0
//            Log.i("MyTAG","*****      Updating a User      **********")
//            userDao.updateUser(UserData(user.id,user.username,user.public_key, user.private_key, user.money_balance))
//
//
//            val users1 = userDao.getUserData()
//            Log.i("MyTAG","*****   ${users.size} books there *****")
//            var user1 = users1[0]
//            Log.i("MyTAG","id: ${user1.id} name: ${user1.username} public_key: ${user1.public_key} private_key ${user1.private_key} balance: ${user1.money_balance.toString()}")

        }
    }
}


