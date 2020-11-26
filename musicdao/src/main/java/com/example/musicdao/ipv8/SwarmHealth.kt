package com.example.musicdao.ipv8

import nl.tudelft.ipv8.messaging.*
import java.util.*

/**
 * This is both an IPv8 message type and a data container for describing the health of a torrent,
 * identified by its infohash
 */
class SwarmHealth(
    val infoHash: String, // String representation of torrent info hash
    val numPeers: UInt,
    val numSeeds: UInt,
    val timestamp: ULong = Date().time.toULong() // Timestamp is saved as Date.getTime format
) :
    Comparable<SwarmHealth>, Serializable {
    override fun compareTo(other: SwarmHealth): Int {
        if (numSeeds < other.numSeeds) return -1
        if (numSeeds == other.numSeeds) return 0
        return 1
    }

    override fun serialize(): ByteArray {
        return serializeVarLen(infoHash.toByteArray(Charsets.US_ASCII)) +
            serializeUInt(numPeers) +
            serializeUInt(numSeeds) +
            serializeULong(timestamp)
    }

    companion object Deserializer : Deserializable<SwarmHealth> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<SwarmHealth, Int> {
            var localOffset = 0
            val (infoHash, infoHashSize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += infoHashSize

            val numPeers = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE
            val numSeeds = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE
            val timestamp = deserializeULong(buffer, offset + localOffset)
            localOffset += SERIALIZED_ULONG_SIZE
            return Pair(
                SwarmHealth(
                    infoHash.toString(Charsets.US_ASCII),
                    numPeers,
                    numSeeds,
                    timestamp
                ),
                localOffset
            )
        }
    }
}
