package nl.tudelft.trustchain.currencyii.sharedWallet

import com.google.gson.Gson
import com.google.gson.JsonObject
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction

data class SWTransferDoneBlockTD(
    var SW_UNIQUE_ID: String,
    var SW_UNIQUE_PROPOSAL_ID: String,
    var SW_TRANSACTION_SERIALIZED: String,
    var SW_BITCOIN_PKS: List<String>,
    var SW_TRANSFER_FUNDS_AMOUNT: Long,
    var SW_TRANSFER_FUNDS_TARGET_SERIALIZED: String
)

class SWTransferDoneTransactionData(data: JsonObject) : SWBlockTransactionData(
    data, CoinCommunity.TRANSFER_FINAL_BLOCK
) {
    fun getData(): SWTransferFundsAskBlockTD {
        return Gson().fromJson(getJsonString(), SWTransferFundsAskBlockTD::class.java)
    }

    constructor(
        uniqueId: String,
        transactionSerialized: String,
        satoshiAmount: Long,
        bitcoinPks: List<String>,
        transferFundsAddressSerialized: String,
        uniqueProposalId: String = SWUtil.randomUUID()
    ) : this(
        SWUtil.objectToJsonObject(
            SWTransferDoneBlockTD(
                uniqueId,
                uniqueProposalId,
                transactionSerialized,
                bitcoinPks,
                satoshiAmount,
                transferFundsAddressSerialized
            )

        )
    )

    constructor(transaction: TrustChainTransaction) : this(SWUtil.parseTransaction(transaction))
}
