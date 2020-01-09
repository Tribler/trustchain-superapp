package nl.tudelft.ipv8

import nl.tudelft.ipv8.keyvault.Key
import java.util.*
import kotlin.math.max

class Peer(
    /**
     * The peer's key.
     */
    val key: Key,

    /**
     * The address of this peer.
     */
    val address: Address = Address("0.0.0.0", 0),

    /**
     * Is this peer suggested to us (otherwise it contacted us).
     */
    val intro: Boolean = true
) {
    private var _lamportTimestamp = 0uL
    val lamportTimestamp = _lamportTimestamp

    val publicKey = key.pub()

    val mid: String = key.keyToHash()

    var lastResponse: Date? = if (intro) null else Date()

    /**
     * Update the Lamport timestamp for this peer. The Lamport clock dictates that the current timestamp is
     * the maximum of the last known and the most recently delivered timestamp. This is useful when messages
     * are delivered asynchronously.
     *
     * We also keep a real time timestamp of the last received message for timeout purposes.
     *
     * @param timestamp A received timestamp.
     */
    fun updateClock(timestamp: ULong) {
        _lamportTimestamp = max(_lamportTimestamp, timestamp)
        lastResponse = Date()
    }
}
