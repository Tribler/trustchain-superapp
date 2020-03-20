package nl.tudelft.ipv8.android.demo.sharedWallet

import nl.tudelft.ipv8.android.demo.CoinCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import org.json.JSONObject

class SWJoinBlockTransactionData(data: JSONObject) : SWBlockTransactionData(
    data, CoinCommunity.SHARED_WALLET_BLOCK
) {
    fun getUniqueId(): String {
        return jsonData.getString(CoinCommunity.SW_UNIQUE_ID)
    }

    fun getEntranceFee(): Long {
        return jsonData.getLong(CoinCommunity.SW_ENTRANCE_FEE)
    }

    fun getTransactionSerialized(): String {
        return jsonData.getString(CoinCommunity.SW_TRANSACTION_SERIALIZED)
    }

    fun getThreshold(): Int {
        return jsonData.getInt(CoinCommunity.SW_VOTING_THRESHOLD)
    }

    fun getTrustChainPks(): ArrayList<String> {
        return SWUtil.parseJSONArray(jsonData.getJSONArray(CoinCommunity.SW_TRUSTCHAIN_PKS))
    }

    fun getBitcoinPks(): ArrayList<String> {
        return SWUtil.parseJSONArray(jsonData.getJSONArray(CoinCommunity.SW_BITCOIN_PKS))
    }

    fun addTrustChainPk(publicKey: String) {
        val union = getTrustChainPks()
        union.add(publicKey)
        jsonData.put(CoinCommunity.SW_TRUSTCHAIN_PKS, union)
    }

    fun addBitcoinPk(publicKey: String) {
        val union = getBitcoinPks()
        union.add(publicKey)
        jsonData.put(CoinCommunity.SW_BITCOIN_PKS, union)
    }

    constructor(
        entranceFee: Long,
        transactionSerialized: String,
        votingThreshold: Int,
        trustChainPks: List<String>,
        bitcoinPks: List<String>,
        uniqueId: String? = null
    ) : this(JSONObject(
        mapOf(
            CoinCommunity.SW_UNIQUE_ID to (uniqueId ?: SWUtil.randomUUID()),
            CoinCommunity.SW_ENTRANCE_FEE to entranceFee,
            CoinCommunity.SW_TRANSACTION_SERIALIZED to transactionSerialized,
            CoinCommunity.SW_VOTING_THRESHOLD to votingThreshold,
            CoinCommunity.SW_TRUSTCHAIN_PKS to trustChainPks,
            CoinCommunity.SW_BITCOIN_PKS to bitcoinPks
        )
    ))

    constructor(transaction: TrustChainTransaction) : this(SWUtil.parseTransaction(transaction))
}


