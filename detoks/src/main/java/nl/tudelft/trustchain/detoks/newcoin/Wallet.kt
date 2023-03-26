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
        private fun create(context: Context, publicKey : nl.tudelft.ipv8.keyvault.PublicKey,
                   privateKey: nl.tudelft.ipv8.keyvault.PrivateKey ): Wallet {
//            val generator = KeyPairGenerator.getInstance("RSA")
//            generator.initialize(2048)
//            val keyPair = generator.generateKeyPair()

            val dbHelper = DbHelper(context)
            val tokens = dbHelper.getAllTokens()
            val listOfFriends = dbHelper.getAllFriends()

            return Wallet(publicKey, privateKey, tokens, listOfFriends)
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
        dbHelper.addFriend(friend.username, friend.publicKey.toString())
    }

    fun addToken(token : Token) {
        tokens.add(token)
        dbHelper.addToken(token)
    }

    fun removeToken(token: Token): Token{
        tokens.remove(token)
        dbHelper.removeToken(token)
        return token
    }


    // Return the number of tokens needed to pay the value
    // For example: we need to pay 2 euros, but we have tokes of 0.50 cents, 1 euro token
    // This means we need to return either two 1 euro tokens or 4x0.50 cents tokens 
    fun getPayment(value: Byte): ArrayList<Token> {
        val tokensToPay = arrayListOf<Token>();
        val tempValue = 0;
        for(Token t: tokens) {
            if(tempValue + t.value <= value){
                tempValue = tempValue + t.value
                tokensToPay.add(removeToken(t))
            }
            if(tempValue == value) break;
        }
        return tokensToPay
    }

// Update token for the admin maybe??

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