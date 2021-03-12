package nl.tudelft.trustchain.currencyii.sharedWallet

import com.google.gson.Gson
import com.google.gson.JsonObject
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.trustchain.currencyii.CoinCommunity

data class SWResponseNegativeSignatureBlockTD(
    var SW_UNIQUE_ID: String,
    var SW_UNIQUE_PROPOSAL_ID: String,
    var SW_SIGNATURE_SERIALIZED: String
)

class SWResponseNegativeSignatureTransactionData(data: JsonObject) : SWBlockTransactionData(
    data, CoinCommunity.SIGNATURE_AGREEMENT_NEGATIVE_BLOCK
) {
    fun getData(): SWResponseNegativeSignatureBlockTD {
        return Gson().fromJson(getJsonString(), SWResponseNegativeSignatureBlockTD::class.java)
    }

    fun matchesProposal(walletId: String, proposalId: String): Boolean {
        val data = getData()
        return data.SW_UNIQUE_ID == walletId && data.SW_UNIQUE_PROPOSAL_ID == proposalId
    }

    constructor(
        uniqueId: String,
        uniqueProposalId: String,
        signatureSerialized: String
    ) : this(
        SWUtil.objectToJsonObject(
            SWResponseNegativeSignatureBlockTD(
                uniqueId,
                uniqueProposalId,
                signatureSerialized
            )
        )

    )

    constructor(transaction: TrustChainTransaction) : this(SWUtil.parseTransaction(transaction))
}
