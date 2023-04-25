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


    val balance: Int get() {
        return tokens!!.size
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
    fun getPayment(value: Int): ArrayList<Token>? {
        val tokensToPay = arrayListOf<Token>()
        var tempValue = 0

        for(t in tokens!!) {
            if(tempValue + t.value <= value){
                tempValue = tempValue + t.value

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

//    fun sendFundsTo(recipient: PublicKey, amountToSend: Int) : Transaction {
//
//        if (amountToSend > balance) {
//            throw IllegalArgumentException("Insufficient funds")
//        }
//
//        val tx = Transaction.create(sender = publicKey, recipient = publicKey, amount = amountToSend)
//        tx.outputs.add(TransactionOutput(recipient = recipient, amount = amountToSend, transactionHash = tx.hash))
//
//        var collectedAmount = 0
//        for (myTx in getMyTransactions()) {
//            collectedAmount += myTx.amount
//            tx.inputs.add(myTx)
//
//            if (collectedAmount > amountToSend) {
//                val change = collectedAmount - amountToSend
//                tx.outputs.add(TransactionOutput(recipient = publicKey, amount = change, transactionHash = tx.hash))
//            }
//
//            if (collectedAmount >= amountToSend) {
//                break
//            }
//        }
//        return tx.sign(privateKey)
//    }
}
