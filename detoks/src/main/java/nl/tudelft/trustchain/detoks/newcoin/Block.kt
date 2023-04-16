package nl.tudelft.trustchain.detoks.newcoin // import java.time.Instant
//
// /**
// * Every block holds a list of transactions
// */
// data class Block(val previousHash: String,
//                 val blockTransactions: MutableList<Transaction> = mutableListOf(),
//                 val timestamp: Long = Instant.now().toEpochMilli(),
//                 val nonce: Long = 0,
//                 var hash: String = "") {
//
//    init {
//        hash = calculateHash()
//    }
//
//    fun calculateHash(): String {
//        return "$previousHash$blockTransactions$timestamp$nonce".hash()
//    }
//
//    fun addTransaction(newTransaction: Transaction) : Block {
//
//        if (newTransaction.isValidSignature())
//            blockTransactions.add(newTransaction)
//        return this
//    }
// }
