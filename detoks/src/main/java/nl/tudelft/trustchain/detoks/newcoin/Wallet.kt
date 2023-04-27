import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import nl.tudelft.trustchain.detoks.Token
import nl.tudelft.trustchain.detoks.db.DbHelper
import nl.tudelft.trustchain.detoks.newcoin.OfflineFriend
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey

class Wallet(
    val publicKey: nl.tudelft.ipv8.keyvault.PublicKey,
    val privateKey: nl.tudelft.ipv8.keyvault.PrivateKey,
) {
    val values: ArrayList<Double> = arrayListOf<Double>(0.05, 0.5, 1.0, 2.0, 5.0)
    companion object{
        private var wallet :Wallet? = null
        private var dbHelper: DbHelper? = null
        private var tokens : MutableList<Token>? = null
        private var listOfFriends : MutableList<OfflineFriend>? = null
        @RequiresApi(Build.VERSION_CODES.O)
        private fun create(context: Context, publicKey : nl.tudelft.ipv8.keyvault.PublicKey,
                           privateKey: nl.tudelft.ipv8.keyvault.PrivateKey ): Wallet {

            dbHelper = DbHelper(context)
            tokens = dbHelper!!.getAllTokens()
            listOfFriends = dbHelper!!.getAllFriends()
            wallet = Wallet(publicKey, privateKey)

            return wallet!!
        }
        @RequiresApi(Build.VERSION_CODES.O)
        fun getInstance(context: Context, publicKey: nl.tudelft.ipv8.keyvault.PublicKey,
                        privateKey: nl.tudelft.ipv8.keyvault.PrivateKey): Wallet {
            return wallet ?: create(context, publicKey, privateKey)
        }
    }

    fun getTokens(): MutableList<Token> {
        return tokens!!
    }

//    fun setTokens(inputTokens : MutableList<Token>) {
//        tokens = inputTokens
//    }

    fun getListOfFriends(): MutableList<OfflineFriend> {
        return listOfFriends!!
    }

//    fun setListOfFriends(inputListOfFriends : MutableList<OfflineFriend>) {
//        listOfFriends = inputListOfFriends
//    }


    val balance: Double get() {
        var sum : Double = 0.0
        for(t in tokens!!){
            sum += values.get(t.value.toInt())
        }
        return sum
    }

    fun getTokensPerValue(tokenValue: Double) : Int {
        val index = values.indexOf(tokenValue)
        var counter = 0
        for(t in tokens!!){
            if(t.value.toInt() == index){
                counter += 1
            }
        }
        return counter
    }

    public fun addFriend(friend: OfflineFriend): Long{
        val result = dbHelper?.addFriend(friend.username, friend.publicKey)
        if(result != -1L) {
            listOfFriends!!.add(friend)
        }
        return result!!
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public fun addToken(token : Token) : Long {
        val result = dbHelper!!.addToken(token)
        if(result != -1L) {
            tokens!!.add(token)
        }
        return result
    }

    /**
     * Returns true if the token was successfully removed
     * false - if the token was not present
     */
    fun removeToken(token: Token): Boolean {
        if(tokens!!.remove(token)) { // returns false if the element was not present in the collection
            dbHelper?.removeToken(token)
            return true
        }
        return false
    }

    // Return the number of tokens needed to pay the value
    // For example: we need to pay 2 euros, but we have tokes of 0.50 cents, 1 euro token
    // This means we need to return either two 1 euro tokens or 4x0.50 cents tokens
//    TODO: This method will not manage to get the right tokens always
    @Synchronized
    fun getPayment(value: Double): ArrayList<Token>? {
        val tokensToPay = arrayListOf<Token>()
        var tempValue = 0.0

        for(t in tokens!!) {
            val valueToken = values.get(t.value.toInt())
            if(tempValue + valueToken <= value){
                tempValue = tempValue + valueToken

                tokensToPay.add(t)

            }
            if(tempValue == value) {
                for (token in tokensToPay)
                    removeToken(token)
                return tokensToPay
            }
        }

        if(tempValue == value) {
            for (token in tokensToPay)
                removeToken(token)
            return tokensToPay
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getPaymentSecond(value: Double, currentValue: Double,
                   currentTokens: ArrayList<Token>): ArrayList<Token>?{
        if(value == currentValue)
            return currentTokens
//        val availableTokens = tokens!!.map{ it.copy()}
        for(t in tokens!!){
            val valueToken = values.get(t.value.toInt())
//            var newCurrentTokens = currentTokens.map { it.copy()}
            currentTokens.add(t)
            removeToken(t)
            getPaymentSecond(value - valueToken, currentValue + valueToken,
                currentTokens)
            addToken(t)
            currentTokens.remove(t)
        }
        return null //if it could not find tokens that sum up to the required value
    }


}
