package nl.tudelft.ipv8.android.demo.sharedWallet

import nl.tudelft.ipv8.android.demo.CoinCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import org.json.JSONObject

class SWResponseSignatureTransactionData(data: JSONObject) : SWBlockTransactionData(
    data, CoinCommunity.SIGNATURE_AGREEMENT_BLOCK
) {
    fun getUniqueId(): String {
        return jsonData.getString(CoinCommunity.SW_UNIQUE_ID)
    }

    fun getUniqueProposalId(): String {
        return jsonData.getString(CoinCommunity.SW_UNIQUE_PROPOSAL_ID)
    }

    fun getSignatureSerialized(): String {
        return jsonData.getString(CoinCommunity.SW_SIGNATURE_SERIALIZED)
    }

    fun matchesProposal(walletId: String, proposalId: String): Boolean {
        return getUniqueId() == walletId && getUniqueProposalId() == proposalId
    }

    constructor(
        uniqueId: String,
        uniqueProposalId: String,
        signatureSerialized: String
    ) : this(
        JSONObject(
            mapOf(
                CoinCommunity.SW_UNIQUE_ID to uniqueId,
                CoinCommunity.SW_UNIQUE_PROPOSAL_ID to uniqueProposalId,
                CoinCommunity.SW_SIGNATURE_SERIALIZED to signatureSerialized
            )
        )
    )

    constructor(transaction: TrustChainTransaction) : this(SWUtil.parseTransaction(transaction))
}
