package nl.tudelft.ipv8.messaging.payload

import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.serializeULong

class GlobalTimeDistributionPayload constructor(
    val globalTime: ULong
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeULong(globalTime)
    }
}
