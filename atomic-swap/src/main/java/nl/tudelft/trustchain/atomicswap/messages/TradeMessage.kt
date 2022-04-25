package nl.tudelft.trustchain.atomicswap.messages

import android.util.Log
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.atomicswap.ui.swap.LOG

data class TradeMessage(val offerId: String, val fromCoin: String, val toCoin: String, val fromAmount: String, val toAmount: String) : Serializable {
    override fun serialize(): ByteArray {
        val msgString = "$offerId;$fromCoin;$toCoin;$fromAmount;$toAmount;"
        Log.v(LOG, "1234 " + msgString.toByteArray().toHex())
        return msgString.toByteArray()
    }

    companion object Deserializer : Deserializable<TradeMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<TradeMessage, Int> {
            val (offerId, fromCoin, toCoin, fromAmount, toAmount) = buffer.drop(offset).toByteArray().decodeToString().split(";")
            return Pair(TradeMessage(offerId, fromCoin, toCoin, fromAmount, toAmount), buffer.size)
        }
    }
}
