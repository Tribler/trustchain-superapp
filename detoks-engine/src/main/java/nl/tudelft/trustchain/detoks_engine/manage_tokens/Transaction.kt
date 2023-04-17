package nl.tudelft.trustchain.detoks_engine.manage_tokens

import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import java.util.*

class Transaction(
    val tokens: List<String>,
    val transactionId: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toTrustChainTransaction() : TrustChainTransaction {
        return mapOf("transactionId" to transactionId, "tokens" to tokens, "createdAt" to createdAt)
    }

    companion object {
        fun fromTrustChainTransactionObject(trustChainTransaction: TrustChainTransaction): Transaction {
            @Suppress("UNCHECKED_CAST")
            val tokens = trustChainTransaction["tokens"] as List<String>
            val transactionId = trustChainTransaction["transactionId"] as String
            val createdAt = trustChainTransaction["createdAt"] as Long
            return Transaction(tokens, transactionId, createdAt)
        }
    }


}
