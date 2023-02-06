package nl.tudelft.trustchain.musicdao.core.ipv8

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
        if (numSeeds + numPeers < other.numSeeds + other.numPeers) return -1
        if (numSeeds + numPeers == other.numSeeds + other.numPeers) return 0
        return 1
    }

    override fun serialize(): ByteArray {
        return serializeVarLen(infoHash.toByteArray(Charsets.US_ASCII)) +
            serializeUInt(numPeers) +
            serializeUInt(numSeeds) +
            serializeULong(timestamp)
    }

    /**
     * Test if all properties are equal of two objects; mainly useful for unit testing
     */
    override fun equals(other: Any?): Boolean {
        if (other !is SwarmHealth) return false
        return infoHash == other.infoHash && numPeers == other.numPeers &&
            numSeeds == other.numSeeds && timestamp == other.timestamp
    }

    /**
     * Check whether the swarm health data is outdated
     */
    fun isUpToDate(): Boolean {
        val timestamp = Date(this.timestamp.toLong())
        if (timestamp.before(Date(Date().time - 3600 * KEEP_TIME_HOURS * 1000))) {
            return false
        }
        return true
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

        /**
         * Pick the best SwarmHealth item to keep; by comparing two objects
         */
        fun pickBest(shLocal: SwarmHealth?, shRemote: SwarmHealth?): SwarmHealth? {
            if (shLocal != null && shRemote != null) {
                return if (shLocal > shRemote) {
                    shLocal
                } else {
                    shRemote
                }
            } else {
                if (shLocal != null) {
                    return shLocal
                }
                if (shRemote != null) {
                    return shRemote
                }
            }
            return null
        }

        // The time, in hours, that we keep this object and see it as up-to-date
        const val KEEP_TIME_HOURS = 2
    }
}
