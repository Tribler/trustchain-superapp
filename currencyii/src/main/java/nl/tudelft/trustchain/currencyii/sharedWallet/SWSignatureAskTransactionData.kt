package nl.tudelft.trustchain.currencyii.sharedWallet

import com.google.gson.Gson
import com.google.gson.JsonObject
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction

data class SWSignatureAskBlockTD(
    var SW_UNIQUE_ID: String,
    var SW_UNIQUE_PROPOSAL_ID: String,
    var SW_TRANSACTION_SERIALIZED: String,
    var SW_TRANSACTION_SERIALIZED_OLD: String,
    var SW_SIGNATURES_REQUIRED: Int,
    var SW_RECEIVER_PK: String
)

open class SWSignatureAskTransactionData(data: JsonObject) : SWBlockTransactionData(
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
        receiverPk: String,
        uniqueProposalId: String = SWUtil.randomUUID()
    ) : this(
        SWUtil.objectToJsonObject(
            SWSignatureAskBlockTD(
                uniqueId,
                uniqueProposalId,
                transactionSerialized,
                oldTransactionSerialized,
                requiredSignatures,
                receiverPk
            )

        )
    )

    constructor(transaction: TrustChainTransaction) : this(SWUtil.parseTransaction(transaction))
}
