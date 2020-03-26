package nl.tudelft.ipv8.android.demo.sharedWallet

import com.google.gson.Gson
import nl.tudelft.ipv8.android.demo.CoinCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import org.json.JSONObject

data class SWJoinBlockTD(
    var SW_UNIQUE_ID: String,
    var SW_ENTRANCE_FEE: Long,
    var SW_TRANSACTION_SERIALIZED: String,
    var SW_VOTING_THRESHOLD: Int,
    var SW_TRUSTCHAIN_PKS: ArrayList<String>,
    var SW_BITCOIN_PKS: ArrayList<String>
)

class SWJoinBlockTransactionData(data: JSONObject) : SWBlockTransactionData(
    data, CoinCommunity.SHARED_WALLET_BLOCK
) {
    fun getData(): SWJoinBlockTD {
        return Gson().fromJson(getJsonString(), SWJoinBlockTD::class.java)
    }

    fun addTrustChainPk(publicKey: String) {
        val data = getData()
        data.SW_TRUSTCHAIN_PKS.add(publicKey)
        jsonData = JSONObject(Gson().toJson(data))
    }

    fun addBitcoinPk(publicKey: String) {
        val data = getData()
        data.SW_BITCOIN_PKS.add(publicKey)
        jsonData = JSONObject(Gson().toJson(data))
    }

    constructor(
        entranceFee: Long,
        transactionSerialized: String,
        votingThreshold: Int,
        trustChainPks: ArrayList<String>,
        bitcoinPks: ArrayList<String>,
        uniqueId: String = SWUtil.randomUUID()
    ) : this(
        JSONObject(
            Gson().toJson(
                SWJoinBlockTD(
                    uniqueId,
                    entranceFee,
                    transactionSerialized,
                    votingThreshold,
                    trustChainPks,
                    bitcoinPks
                )
            )
        )
    )

    constructor(transaction: TrustChainTransaction) : this(SWUtil.parseTransaction(transaction))
}


