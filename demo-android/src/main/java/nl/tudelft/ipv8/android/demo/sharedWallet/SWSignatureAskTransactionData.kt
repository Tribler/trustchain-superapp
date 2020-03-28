package nl.tudelft.ipv8.android.demo.sharedWallet

import com.google.gson.Gson
import nl.tudelft.ipv8.android.demo.CoinCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import org.json.JSONObject

data class SWSignatureAskBlockTD(
    var SW_UNIQUE_ID: String,
    var SW_UNIQUE_PROPOSAL_ID: String,
    var SW_TRANSACTION_SERIALIZED: String,
    var SW_TRANSACTION_SERIALIZED_OLD: String,
    var SW_SIGNATURES_REQUIRED: Int
)

open class SWSignatureAskTransactionData(data: JSONObject) : SWBlockTransactionData(
    data, CoinCommunity.SIGNATURE_ASK_BLOCK
) {
    fun getData(): SWSignatureAskBlockTD {
        return Gson().fromJson(getJsonString(), SWSignatureAskBlockTD::class.java)
    }

    constructor(
        uniqueId: String,
        transactionSerialized: String,
        oldTransactionSerialized: String,
        requiredSignatures: Int,
        uniqueProposalId: String = SWUtil.randomUUID()
    ) : this(
        JSONObject(
            Gson().toJson(
                SWSignatureAskBlockTD(
                    uniqueId,
                    uniqueProposalId,
                    transactionSerialized,
                    oldTransactionSerialized,
                    requiredSignatures
                )
            )
        )
    )

    constructor(transaction: TrustChainTransaction) : this(SWUtil.parseTransaction(transaction))
}
