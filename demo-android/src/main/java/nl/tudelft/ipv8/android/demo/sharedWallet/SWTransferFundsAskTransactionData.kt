package nl.tudelft.ipv8.android.demo.sharedWallet

import com.google.gson.Gson
import nl.tudelft.ipv8.android.demo.CoinCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import org.json.JSONObject

data class SWTransferFundsAskBlockTD(
    var SW_UNIQUE_ID: String,
    var SW_UNIQUE_PROPOSAL_ID: String,
    var SW_TRANSACTION_SERIALIZED_OLD: String,
    var SW_BITCOIN_PKS: List<String>,
    var SW_SIGNATURES_REQUIRED: Int,
    var SW_TRANSFER_FUNDS_AMOUNT: Long,
    var SW_TRANSFER_FUNDS_TARGET_SERIALIZED: String
)

class SWTransferFundsAskTransactionData(data: JSONObject) : SWBlockTransactionData(
    data, CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK
) {
    fun getData(): SWTransferFundsAskBlockTD {
        return Gson().fromJson(getJsonString(), SWTransferFundsAskBlockTD::class.java)
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
            Gson().toJson(
                SWTransferFundsAskBlockTD(
                    uniqueId,
                    uniqueProposalId,
                    oldTransactionSerialized,
                    bitcoinPks,
                    requiredSignatures,
                    satoshiAmount,
                    transferFundsAddressSerialized
                )
            )
        )
    )

    constructor(transaction: TrustChainTransaction) : this(SWUtil.parseTransaction(transaction))
}
