package nl.tudelft.ipv8

import java.util.*
import kotlin.math.max

class Peer(
    val key: ByteArray, // TODO
    val address: Address,
    val intro: Boolean = true
) {
    private var _lamportTimestamp = 0uL
    val lamportTimestamp = _lamportTimestamp

    // TODO
    val publicKey: ByteArray = key

    // TODO: key hash
    val mid: String = key.toString()

    var lastResponse: Date? = if (intro) null else Date()

    fun updateClock(timestamp: ULong) {
        _lamportTimestamp = max(_lamportTimestamp, timestamp)
        lastResponse = Date()
    }
}
