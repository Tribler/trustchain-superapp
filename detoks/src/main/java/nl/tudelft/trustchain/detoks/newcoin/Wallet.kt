import android.content.Context
import nl.tudelft.trustchain.detoks.Token
import nl.tudelft.trustchain.detoks.db.DbHelper
import nl.tudelft.trustchain.detoks.newcoin.OfflineFriend
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey

data class Wallet(
    val publicKey: nl.tudelft.ipv8.keyvault.PublicKey,
    val privateKey: nl.tudelft.ipv8.keyvault.PrivateKey,
    val tokens: MutableList<Token>,
    val listOfFriends: MutableList<String>
) {

    companion object {
        private var wallet :Wallet? = null
        private var dbHelper: DbHelper? = null;
        private fun create(context: Context, publicKey : nl.tudelft.ipv8.keyvault.PublicKey,
                           privateKey: nl.tudelft.ipv8.keyvault.PrivateKey ): Wallet {

            dbHelper = DbHelper(context)
//            val tokens = dbHelper.getAllTokens()
            var listOfFriends = dbHelper?.getAllFriends()
            wallet = Wallet(publicKey, privateKey, mutableListOf<Token>(), listOfFriends!!)

            return wallet as Wallet
        }
        fun getInstance(context: Context, publicKey: nl.tudelft.ipv8.keyvault.PublicKey,
                        privateKey: nl.tudelft.ipv8.keyvault.PrivateKey): Wallet {
            return this.wallet ?: create(context, publicKey, privateKey)
        }
    }

    val balance: Int get() {
        return tokens.size
    }

//    private fun getMyTransactions() : Collection<TransactionOutput> {
//        return blockChain.UTXO.filterValues { it.isMine(publicKey) }.values
//    }

    public fun addFriend(friend: OfflineFriend): Long{
        val result = dbHelper!!.addFriend(friend.username, friend.publicKey)
        if(result != -1L) {
            listOfFriends.add(friend.username)
        }
        return result
    }

    public fun addToken(token : Token) {
        tokens.add(token)
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
