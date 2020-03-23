package nl.tudelft.ipv8.android.demo.sharedWallet

import nl.tudelft.ipv8.android.demo.CoinCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import org.json.JSONObject

open class SWSignatureAskTransactionData(data: JSONObject) : SWBlockTransactionData(
    data, CoinCommunity.SIGNATURE_ASK_BLOCK
) {
    fun getUniqueId(): String {
        return jsonData.getString(CoinCommunity.SW_UNIQUE_ID)
    }

    fun getUniqueProposalId(): String {
        return jsonData.getString(CoinCommunity.SW_UNIQUE_PROPOSAL_ID)
    }

    fun getTransactionSerialized(): String {
        return jsonData.getString(CoinCommunity.SW_TRANSACTION_SERIALIZED)
    }

    fun getOldTransactionSerialized(): String {
        return jsonData.getString(CoinCommunity.SW_TRANSACTION_SERIALIZED_OLD)
    }

    fun getRequiredSignatures(): Int {
        return jsonData.getInt(CoinCommunity.SW_SIGNATURES_REQUIRED)
    }

    constructor(
        uniqueId: String,
        transactionSerialized: String,
        oldTransactionSerialized: String,
        requiredSignatures: Int,
        uniqueProposalId: String = SWUtil.randomUUID()
    ) : this(
        JSONObject(
            mapOf(
                CoinCommunity.SW_UNIQUE_ID to uniqueId,
                CoinCommunity.SW_UNIQUE_PROPOSAL_ID to uniqueProposalId,
                CoinCommunity.SW_TRANSACTION_SERIALIZED to transactionSerialized,
                CoinCommunity.SW_TRANSACTION_SERIALIZED_OLD to oldTransactionSerialized,
                CoinCommunity.SW_SIGNATURES_REQUIRED to requiredSignatures
            )
        )
    )

    constructor(transaction: TrustChainTransaction) : this(SWUtil.parseTransaction(transaction))
}
