package nl.tudelft.trustchain.musicdao.core.util.sharedWallet

import com.google.gson.Gson
import com.google.gson.JsonObject
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.trustchain.musicdao.core.dao.CoinCommunity

data class SWTransferDoneBlockTD(
    var SW_UNIQUE_ID: String,
    var SW_UNIQUE_PROPOSAL_ID: String,
    var SW_TRANSACTION_SERIALIZED: String,
    var SW_TRUSTCHAIN_PKS: ArrayList<String>,
    var SW_BITCOIN_PKS: ArrayList<String>,
    var SW_NONCE_PKS: ArrayList<String>,
    var SW_TRANSFER_FUNDS_AMOUNT: Long,
    var SW_TRANSFER_FUNDS_TARGET_SERIALIZED: String
)

class SWTransferDoneTransactionData(data: JsonObject) : SWBlockTransactionData(
    data,
    CoinCommunity.TRANSFER_FINAL_BLOCK
) {
    fun getData(): SWTransferDoneBlockTD {
        return Gson().fromJson(getJsonString(), SWTransferDoneBlockTD::class.java)
    }

    fun setTransactionSerialized(serializedTransaction: String) {
        val data = getData()
        data.SW_TRANSACTION_SERIALIZED = serializedTransaction
        jsonData = SWUtil.objectToJsonObject(data)
    }

    constructor(transaction: TrustChainTransaction) : this(SWUtil.parseTransaction(transaction))
}
