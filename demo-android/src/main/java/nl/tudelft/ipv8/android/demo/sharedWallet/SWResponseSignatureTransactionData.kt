package nl.tudelft.ipv8.android.demo.sharedWallet

import nl.tudelft.ipv8.android.demo.CoinCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import org.json.JSONObject

class SWResponseSignatureTransactionData(data: JSONObject) : SWBlockTransactionData(
    data, CoinCommunity.SIGNATURE_AGREEMENT_BLOCK
) {
    fun getUniqueId(): String {
        return jsonData.getString(CoinCommunity.SW_UNIQUE_ID)
    }

    fun getSignatureSerialized(): String {
        return jsonData.getString(CoinCommunity.SW_SIGNATURE_SERIALIZED)
    }

    constructor(
        uniqueId: String,
        signatureSerialized: String
    ) : this(
        JSONObject(
            mapOf(
                CoinCommunity.SW_UNIQUE_ID to uniqueId,
                CoinCommunity.SW_SIGNATURE_SERIALIZED to signatureSerialized
            )
        )
    )

    constructor(transaction: TrustChainTransaction) : this(SWUtil.parseTransaction(transaction))
}
