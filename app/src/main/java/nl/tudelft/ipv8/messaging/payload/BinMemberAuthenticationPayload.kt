package nl.tudelft.ipv8.messaging.payload

import nl.tudelft.ipv8.messaging.Serializable

class BinMemberAuthenticationPayload(
    val publicKey: String
) : Serializable {
    override fun serialize(): ByteArray {
        // TODO: check how to serialize string
        // TODO: deserialize
        return publicKey.toByteArray()
    }
}
