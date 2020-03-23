package nl.tudelft.ipv8.android.demo.sharedWallet

import nl.tudelft.ipv8.android.demo.CoinCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import org.json.JSONObject

class SWTransferFundsAskBlockData(data: JSONObject) : SWBlockTransactionData(
    data, CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK
) {
    fun getUniqueId(): String {
        return jsonData.getString(CoinCommunity.SW_UNIQUE_ID)
    }

    fun getUniqueProposalId(): String {
        return jsonData.getString(CoinCommunity.SW_UNIQUE_PROPOSAL_ID)
    }

    fun getOldTransactionSerialized(): String {
        return jsonData.getString(CoinCommunity.SW_TRANSACTION_SERIALIZED_OLD)
    }

    fun getRequiredSignatures(): Int {
        return jsonData.getInt(CoinCommunity.SW_SIGNATURES_REQUIRED)
    }

    fun getBitcoinPks(): ArrayList<String> {
        return SWUtil.parseJSONArray(jsonData.getJSONArray(CoinCommunity.SW_BITCOIN_PKS))
    }

    fun getSatoshiAmount(): Long {
        return jsonData.getLong(CoinCommunity.SW_TRANSFER_FUNDS_AMOUNT)
    }

    fun getTransferFundsTargetSerialized(): String {
        return jsonData.getString(CoinCommunity.SW_TRANSFER_FUNDS_TARGET_SERIALIZED)
    }

    constructor(
        uniqueId: String,
        oldTransactionSerialized: String,
        requiredSignatures: Int,
        satoshiAmount: Long,
        bitcoinPks: List<String>,
        transferFundsAddressSerialized: String,
        uniqueProposalId: String = SWUtil.randomUUID()
    ) : this(
        JSONObject(
            mapOf(
                CoinCommunity.SW_UNIQUE_ID to uniqueId,
                CoinCommunity.SW_UNIQUE_PROPOSAL_ID to uniqueProposalId,
                CoinCommunity.SW_TRANSACTION_SERIALIZED_OLD to oldTransactionSerialized,
                CoinCommunity.SW_BITCOIN_PKS to bitcoinPks,
                CoinCommunity.SW_SIGNATURES_REQUIRED to requiredSignatures,
                CoinCommunity.SW_TRANSFER_FUNDS_AMOUNT to satoshiAmount, // Only new value of this class
                CoinCommunity.SW_TRANSFER_FUNDS_TARGET_SERIALIZED to transferFundsAddressSerialized
            )
        )
    )

    constructor(transaction: TrustChainTransaction) : this(SWUtil.parseTransaction(transaction))
}
