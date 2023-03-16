//import java.security.PrivateKey
//import java.security.PublicKey
//
//
//data class Transaction(val sender: PublicKey,
//                       val recipient: PublicKey,
//                       val amount: Int,
//                       var hash: String = "",
//                       val inputs: MutableList<TransactionOutput> = mutableListOf(),
//                       val outputs: MutableList<TransactionOutput> = mutableListOf()) {
//
//    private var signature: ByteArray = ByteArray(0)
//
//    init {
//        hash = "${sender.encodeToString()}${recipient.encodeToString()}$amount$salt".hash()
//    }
//
//    companion object {
//        fun create(sender: PublicKey, recipient: PublicKey, amount: Int) : Transaction {
//            return Transaction(sender, recipient, amount)
//        }
//
//        var salt: Long = 0
//            get() {
//                field += 1
//                return field
//            }
//    }
//
//    fun sign(privateKey: PrivateKey) : Transaction {
//        signature = "${sender.encodeToString()}${recipient.encodeToString()}$amount".sign(privateKey)
//        return this
//    }
//
//    fun isSignatureValid() : Boolean {
//        return "${sender.encodeToString()}${recipient.encodeToString()}$amount".verifySignature(sender, signature)
//    }
//}
//
//data class TransactionOutput(val recipient: PublicKey,
//                             val amount: Int,
//                             val transactionHash: String,
//                             var hash: String = "") {
//    init {
//        hash = "${recipient.encodeToString()}$amount$transactionHash".hash()
//    }
//
//    /**
//     * Is the sender of the current coin the same as the reciever?
//     */
//    fun isMine(ownerPublickey: PublicKey) : Boolean {
//        return recipient == ownerPublickey
//    }
//}
//
