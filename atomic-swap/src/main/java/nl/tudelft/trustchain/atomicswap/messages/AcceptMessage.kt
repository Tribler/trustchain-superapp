package nl.tudelft.trustchain.atomicswap.messages

import android.util.Log
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.atomicswap.ui.swap.LOG

data class AcceptMessage(val offerId: String, val btcPubKey: String, val ethAddress: String) : Serializable {
    override fun serialize(): ByteArray {
        val msgString = "$offerId;$btcPubKey;$ethAddress;"
        Log.v(LOG, msgString.toByteArray().toHex())
        return msgString.toByteArray()
    }

    companion object Deserializer : Deserializable<AcceptMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<AcceptMessage, Int> {
            val (offerId, btcKey, ethAddress) = buffer.drop(offset).toByteArray().decodeToString().split(";")
            return Pair(AcceptMessage(offerId, btcKey, ethAddress), buffer.size)
        }
    }
}
