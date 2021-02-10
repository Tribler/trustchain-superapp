package nl.tudelft.trustchain.eurotoken.community

import android.util.Log
import nl.tudelft.ipv8.messaging.*

class RollbackRequestPayload constructor(
    val transactionHash: ByteArray
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(transactionHash)
    }

    companion object Deserializer : Deserializable<RollbackRequestPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<RollbackRequestPayload, Int> {
            Log.d("Deserialise", "start")
            var localOffset = offset
            var (transactionHash, transactionHashSize) = deserializeVarLen(buffer, localOffset)
            localOffset += transactionHashSize

            Log.d("Deserialise", transactionHash.toString())

            return Pair(
                RollbackRequestPayload(
                    transactionHash
                ),
                localOffset - offset
            )
        }
    }
}
