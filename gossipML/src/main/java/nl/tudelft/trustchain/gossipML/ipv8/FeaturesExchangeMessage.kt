package nl.tudelft.trustchain.gossipML.ipv8

import nl.tudelft.ipv8.messaging.*

/**
 * This is a message from a peer sending local songs features to other peers
 */
open class FeaturesExchangeMessage @ExperimentalUnsignedTypes constructor(
    val originPublicKey: ByteArray,
    var ttl: UInt,
    val songIdentifier: String,
    val features: String
) : Serializable {

    override fun serialize(): ByteArray {
        return originPublicKey +
            serializeUInt(ttl) +
            serializeVarLen(songIdentifier.toByteArray(Charsets.UTF_8)) +
            serializeVarLen(features.toByteArray(Charsets.UTF_8))
    }

    @kotlin.ExperimentalUnsignedTypes
    fun checkTTL(): Boolean {
        ttl -= 1u
        if (ttl < 1u) return false
        return true
    }

    companion object Deserializer : Deserializable<FeaturesExchangeMessage> {
        @ExperimentalUnsignedTypes
        @JvmStatic
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<FeaturesExchangeMessage, Int> {
            var localOffset = 0
            val originPublicKey = buffer.copyOfRange(
                offset + localOffset,
                offset + localOffset + SERIALIZED_PUBLIC_KEY_SIZE
            )

            localOffset += SERIALIZED_PUBLIC_KEY_SIZE
            val ttl = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE

            val (songIdentifierBytes, songIdentifierSize) = deserializeVarLen(buffer, offset + localOffset)
            val songIdentifier = songIdentifierBytes.toString(Charsets.UTF_8)
            localOffset += songIdentifierSize
            val (featuresBytes, featuresSize) = deserializeVarLen(buffer, offset + localOffset)
            val features = featuresBytes.toString(Charsets.UTF_8)
            localOffset += featuresSize

            return Pair(
                first = FeaturesExchangeMessage(
                    originPublicKey = originPublicKey,
                    ttl = ttl,
                    songIdentifier = songIdentifier,
                    features = features
                ),
                second = localOffset
            )
        }
    }
}
