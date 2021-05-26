package nl.tudelft.trustchain.gossipML.ipv8

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import nl.tudelft.ipv8.messaging.*
import nl.tudelft.trustchain.gossipML.models.Model
import nl.tudelft.trustchain.gossipML.models.collaborative_filtering.PublicMatrixFactorization
import nl.tudelft.trustchain.gossipML.models.feature_based.Adaline
import nl.tudelft.trustchain.gossipML.models.feature_based.Pegasos

/**
 * This is a message from a peer sending walking model to other peers
 */
open class ModelExchangeMessage @ExperimentalUnsignedTypes constructor(
    val originPublicKey: ByteArray,
    var ttl: UInt,
    val modelType: String,
    val model: Model
) : Serializable {

    override fun serialize(): ByteArray {
        return originPublicKey +
            serializeUInt(ttl) +
            serializeVarLen(modelType.toByteArray(Charsets.UTF_8)) +
            serializeVarLen(model.serialize().toByteArray(Charsets.UTF_8))
    }

    @kotlin.ExperimentalUnsignedTypes
    fun checkTTL(): Boolean {
        ttl -= 1u
        if (ttl < 1u) return false
        return true
    }

    companion object Deserializer : Deserializable<ModelExchangeMessage> {
        @ExperimentalUnsignedTypes
        @JvmStatic
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<ModelExchangeMessage, Int> {
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
            val (modelBytes, modelSize) = deserializeVarLen(buffer, offset + localOffset)
            val modelJsonStr = modelBytes.toString(Charsets.UTF_8)
            localOffset += modelSize

            val model = if (modelType == "Adaline")
                Json.decodeFromString<Adaline>(modelJsonStr)
            else if (modelType == "Pegasos")
                Json.decodeFromString<Pegasos>(modelJsonStr)
            else
                Json.decodeFromString<PublicMatrixFactorization>(modelJsonStr)

            return Pair(
                first = ModelExchangeMessage(
                    originPublicKey = originPublicKey,
                    ttl = ttl,
                    modelType = modelType,
                    model = model,
                ),
                second = localOffset
            )
        }
    }
}
