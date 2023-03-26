import nl.tudelft.trustchain.detoks.Token
import nl.tudelft.trustchain.detoks.newcoin.OfflineFriend
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey

data class Wallet(
    val publicKey: nl.tudelft.ipv8.keyvault.PublicKey,
    val privateKey: nl.tudelft.ipv8.keyvault.PrivateKey,
    val tokens: ArrayList<Token>,
    val listOfFriends: ArrayList<OfflineFriend>
) {


    companion object {
        private var wallet :Wallet? = null;
        private fun create(publicKey : nl.tudelft.ipv8.keyvault.PublicKey,
                   privateKey: nl.tudelft.ipv8.keyvault.PrivateKey ): Wallet {
//            val generator = KeyPairGenerator.getInstance("RSA")
//            generator.initialize(2048)
//            val keyPair = generator.generateKeyPair()

            return Wallet(publicKey, privateKey, arrayListOf<Token>(), arrayListOf<OfflineFriend>())
        }
        fun getInstance(publicKey: nl.tudelft.ipv8.keyvault.PublicKey,
                        privateKey: nl.tudelft.ipv8.keyvault.PrivateKey): Wallet {
            return this.wallet ?: create(publicKey, privateKey)
        }

    }


    val balance: Int get() {
        return tokens.size
    }

    fun addFriend(friend: OfflineFriend){
        listOfFriends.add(friend)
    }

    fun addToken(token : Token) {
        tokens.add(token)
    }

//    fun removeToken(): Token {
//
//    }


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
