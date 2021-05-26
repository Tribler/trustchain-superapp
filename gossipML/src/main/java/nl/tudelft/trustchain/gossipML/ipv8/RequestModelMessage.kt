package nl.tudelft.trustchain.gossipML.ipv8

import nl.tudelft.ipv8.messaging.*

/**
 * This is a message from a peer sending and asking for a model from other peers
 */
open class RequestModelMessage @ExperimentalUnsignedTypes constructor(
    val originPublicKey: ByteArray,
    var ttl: UInt,
    val modelType: String
) : Serializable {

    override fun serialize(): ByteArray {
        return originPublicKey + serializeUInt(ttl)
    }

    @kotlin.ExperimentalUnsignedTypes
    fun checkTTL(): Boolean {
        ttl -= 1u
        if (ttl < 1u) return false
        return true
    }

    companion object Deserializer : Deserializable<RequestModelMessage> {
        @ExperimentalUnsignedTypes
        @JvmStatic
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<RequestModelMessage, Int> {
            var localOffset = 0

            val originPublicKey = buffer.copyOfRange(
                offset + localOffset,
                offset + localOffset + SERIALIZED_PUBLIC_KEY_SIZE
            )
            localOffset += SERIALIZED_PUBLIC_KEY_SIZE

            val ttl = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE

            val (modelTypeBytes, modelTypeSize) = deserializeVarLen(buffer, offset + localOffset)
            val modelType = modelTypeBytes.toString(Charsets.UTF_8)
            localOffset += modelTypeSize

            return Pair(
                first = RequestModelMessage(
                    originPublicKey = originPublicKey,
                    ttl = ttl,
                    modelType = modelType
                ),
                second = localOffset
            )
        }
    }
}
