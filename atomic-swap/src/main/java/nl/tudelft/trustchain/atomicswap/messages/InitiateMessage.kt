package nl.tudelft.trustchain.atomicswap.messages

import android.util.Log
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.atomicswap.ui.swap.LOG

data class InitiateMessage(val offerId: String, val hash:String, val txId: String, val btcPublickey: String, val ethAddress: String) : Serializable {
    override fun serialize(): ByteArray {
        val msgString = "$offerId;$hash;$txId;$btcPublickey;$ethAddress;"
        Log.v(LOG, msgString.toByteArray().toHex())
        return msgString.toByteArray()
    }

    companion object Deserializer : Deserializable<InitiateMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<InitiateMessage, Int> {
            val (offerId, hash, txId, publicKey,ethAddress) = buffer.drop(offset).toByteArray().decodeToString()
                .split(";")
            return Pair(InitiateMessage(offerId, hash, txId, publicKey,ethAddress), buffer.size)
        }
    }
}
