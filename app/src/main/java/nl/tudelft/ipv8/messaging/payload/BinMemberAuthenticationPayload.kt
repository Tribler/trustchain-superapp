package nl.tudelft.ipv8.messaging.payload

import nl.tudelft.ipv8.messaging.Serializable

class BinMemberAuthenticationPayload(
    val publicKey: ByteArray
) : Serializable {
    override fun serialize(): ByteArray {
        return publicKey
    }
}
