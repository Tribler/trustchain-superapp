package nl.tudelft.trustchain.musicdao.core.ipv8.modules.search

import nl.tudelft.ipv8.messaging.*

/**
 * This is a message from a peer asking for music content from other peers, filtered on a specific keyword
 */
class KeywordSearchMessage(
    val originPublicKey: ByteArray,
    var ttl: UInt,
    val keyword: String
) : Serializable {
    override fun serialize(): ByteArray {
        return originPublicKey +
            serializeUInt(ttl) +
            serializeVarLen(keyword.toByteArray(Charsets.US_ASCII))
    }

    fun checkTTL(): Boolean {
        ttl -= 1u
        if (ttl < 1u) return false
        return true
    }

    companion object Deserializer : Deserializable<KeywordSearchMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<KeywordSearchMessage, Int> {
            var localOffset = 0
            val originPublicKey = buffer.copyOfRange(
                offset + localOffset,
                offset + localOffset + SERIALIZED_PUBLIC_KEY_SIZE
            )
            localOffset += SERIALIZED_PUBLIC_KEY_SIZE
            val ttl = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE
            val (keyword, keywordSize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += keywordSize
            return Pair(
                KeywordSearchMessage(
                    originPublicKey,
                    ttl,
                    keyword.toString(Charsets.US_ASCII)
                ),
                localOffset
            )
        }
    }
}
