package nl.tudelft.ipv8.android.demo.sharedWallet

import com.google.gson.Gson
import nl.tudelft.ipv8.android.demo.CoinCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import org.json.JSONObject

data class SWResponseSignatureBlockTD(
    var SW_UNIQUE_ID: String,
    var SW_UNIQUE_PROPOSAL_ID: String,
    var SW_SIGNATURE_SERIALIZED: String
)

class SWResponseSignatureTransactionData(data: JSONObject) : SWBlockTransactionData(
    data, CoinCommunity.SIGNATURE_AGREEMENT_BLOCK
) {
    fun getData(): SWResponseSignatureBlockTD {
        return Gson().fromJson(getJsonString(), SWResponseSignatureBlockTD::class.java)
    }

    fun matchesProposal(walletId: String, proposalId: String): Boolean {
        val data = getData()
        return data.SW_UNIQUE_ID == walletId && data.SW_UNIQUE_PROPOSAL_ID == proposalId
    }

    constructor(
        uniqueId: String,
        uniqueProposalId: String,
        signatureSerialized: String
    ): this(
        JSONObject(
            Gson().toJson(
                SWResponseSignatureBlockTD(
                    uniqueId,
                    uniqueProposalId,
                    signatureSerialized
                )
            )
        )
    )

    constructor(transaction: TrustChainTransaction) : this(SWUtil.parseTransaction(transaction))
}
