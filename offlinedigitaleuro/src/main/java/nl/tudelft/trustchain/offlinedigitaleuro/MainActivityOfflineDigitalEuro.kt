package nl.tudelft.trustchain.offlinedigitaleuro

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.trustchain.common.BaseActivity

class MainActivityOfflineDigitalEuro() : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_offlinedigitaleuro
    override val bottomNavigationMenu = R.menu.offlinedigitaleuro_menu

    private fun testDB() {
        lifecycleScope.launch(Dispatchers.IO) {
//            Log.i("MyTAG","*****     Inserting 1 Token     **********")
//            tokensDao.insertToken(
//                Token(Random.nextInt(0, 10000).toString(), 5.0, "ByteArray".toByteArray())
//            )
//
//            // Query
//            var tokens = tokensDao.getAllTokensOfValue(5.0)
//            Log.i("MyTAG","*****   ${tokens.size} tokens there *****")
//
//            for (token in tokens) {
//                Log.i("MyTAG", "${token.token_id}\t has ${token.token_value}\t with data ${token.token_data}\n")
//            }

//            //Insert
//            Log.i("MyTAG","*****     Inserting 1 User     **********")
//            userDao.insertUser(
//                UserData(0,"Java","Alex", "dfslafk")
//            )
//            Log.i("MyTAG","*****     Inserted 1 User       **********")
//
//            // Query
//            var users = userDao.getUserData()
//            Log.i("MyTAG","*****   ${users.size} users there *****")
//            var user = users[0]
//            Log.i("MyTAG","id: ${user.id} name: ${user.username} public_key: ${user.public_key} private_key ${user.private_key}")
//
////            //Update
//            Log.i("MyTAG","*****      Updating a User      **********")
//            userDao.updateUser(UserData(user.id,"Hello",user.public_key, user.private_key))
////
////
//            val users1 = userDao.getUserData()
//            Log.i("MyTAG","*****   ${users.size} books there *****")
//            var user1 = users1[0]
//            Log.i("MyTAG","id: ${user1.id} name: ${user1.username} public_key: ${user1.public_key} private_key ${user1.private_key}")

        }
    }
}


