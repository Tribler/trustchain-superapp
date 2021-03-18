package nl.tudelft.trustchain.currencyii.sharedWallet

import com.google.gson.Gson
import com.google.gson.JsonObject
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.trustchain.currencyii.CoinCommunity

data class SWTransferFundsAskBlockTD(
    var SW_UNIQUE_ID: String,
    var SW_UNIQUE_PROPOSAL_ID: String,
    var SW_PREVIOUS_BLOCK_HASH: String,
    var SW_BITCOIN_PKS: List<String>,
    var SW_SIGNATURES_REQUIRED: Int,
    var SW_TRANSFER_FUNDS_AMOUNT: Long,
    var SW_TRANSFER_FUNDS_TARGET_SERIALIZED: String,
    var SW_RECEIVER_PK: String
)

class SWTransferFundsAskTransactionData(data: JsonObject) : SWBlockTransactionData(
    data, CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK
) {
    fun getData(): SWTransferFundsAskBlockTD {
        return Gson().fromJson(getJsonString(), SWTransferFundsAskBlockTD::class.java)
    }

    constructor(
        uniqueId: String,
        previousWalletBlockHash: String,
        requiredSignatures: Int,
        satoshiAmount: Long,
        bitcoinPks: List<String>,
        transferFundsAddressSerialized: String,
        receiverPk: String,
        uniqueProposalId: String
    ) : this(
        SWUtil.objectToJsonObject(
            SWTransferFundsAskBlockTD(
                uniqueId,
                uniqueProposalId,
                previousWalletBlockHash,
                bitcoinPks,
                requiredSignatures,
                satoshiAmount,
                transferFundsAddressSerialized,
                receiverPk
            )

        )
    )

    constructor(transaction: TrustChainTransaction) : this(SWUtil.parseTransaction(transaction))
}
