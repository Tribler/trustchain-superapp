package nl.tudelft.trustchain.currencyii.sharedWallet

import com.google.gson.Gson
import com.google.gson.JsonObject
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.trustchain.currencyii.CoinCommunity

data class SWJoinBlockTD(
    var SW_UNIQUE_ID: String,
    var SW_ENTRANCE_FEE: Long,
    var SW_TRANSACTION_SERIALIZED: String,
    var SW_VOTING_THRESHOLD: Int,
    var SW_TRUSTCHAIN_PKS: ArrayList<String>,
    var SW_BITCOIN_PKS: ArrayList<String>,
    var SW_NONCE_PKS: ArrayList<String>
)

class SWJoinBlockTransactionData(data: JsonObject) : SWBlockTransactionData(
    data, CoinCommunity.JOIN_BLOCK
) {
    fun getData(): SWJoinBlockTD {
        return Gson().fromJson(getJsonString(), SWJoinBlockTD::class.java)
    }

    fun addTrustChainPk(publicKey: String) {
        val data = getData()
        data.SW_TRUSTCHAIN_PKS.add(publicKey)
        jsonData = SWUtil.objectToJsonObject(data)
    }

    fun addBitcoinPk(publicKey: String) {
        val data = getData()
        data.SW_BITCOIN_PKS.add(publicKey)
        jsonData = SWUtil.objectToJsonObject(data)
    }

    fun addNoncePk(publicKey: String) {
        val data = getData()
        data.SW_NONCE_PKS.add(publicKey)
        jsonData = SWUtil.objectToJsonObject(data)
    }

    fun setTransactionSerialized(serializedTransaction: String) {
        val data = getData()
        data.SW_TRANSACTION_SERIALIZED = serializedTransaction
        jsonData = SWUtil.objectToJsonObject(data)
    }

    constructor(
        entranceFee: Long,
        transactionSerialized: String,
        votingThreshold: Int,
        trustChainPks: ArrayList<String>,
        bitcoinPks: ArrayList<String>,
        noncePks: ArrayList<String>,
        uniqueId: String = SWUtil.randomUUID()
    ) : this(
        SWUtil.objectToJsonObject(
            SWJoinBlockTD(
                uniqueId,
                entranceFee,
                transactionSerialized,
                votingThreshold,
                trustChainPks,
                bitcoinPks,
                noncePks
            )
        )
    )

    constructor(transaction: TrustChainTransaction) : this(SWUtil.parseTransaction(transaction))
}
