package nl.tudelft.ipv8.messaging

import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.exception.PacketDecodingException
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.messaging.payload.BinMemberAuthenticationPayload
import nl.tudelft.ipv8.messaging.payload.GlobalTimeDistributionPayload
import java.lang.IllegalArgumentException

class Packet(
    val source: Address,
    val data: ByteArray
) {
    /**
     * Deserializes an unauthenticated packet and returns the main payload.
     */
    fun <T> getPayload(deserializer: Deserializable<T>): T {
        val remainder = getPayload()
        val (_, distSize) = GlobalTimeDistributionPayload.deserialize(remainder)
        val (payload, _) = deserializer.deserialize(remainder, distSize)
        return payload
    }

    /**
     * Checks the signature of an authenticated packet payload and throws [PacketDecodingException]
     * if invalid. Returns the peer initialized using the packet source address and the public key
     * from [BinMemberAuthenticationPayload], and the main deserialized payload.
     *
     * @throws PacketDecodingException If the packet is authenticated and the signature is invalid.
     */
    @Throws(PacketDecodingException::class)
    fun <T> getAuthPayload(deserializer: Deserializable<T>): Pair<Peer, T> {
        val (peer, remainder) = getAuthPayload()
        val (_, distSize) = GlobalTimeDistributionPayload.deserialize(remainder)
        val (payload, _) = deserializer.deserialize(remainder, distSize)
        return Pair(peer, payload)
    }

    /**
     * Strips the prefix, message type, and returns the raw payload.
     */
    private fun getPayload(): ByteArray {
        return data.copyOfRange(PREFIX_SIZE + 1, data.size)
    }

    /**
     * Checks the signature of an authenticated packet payload and throws [PacketDecodingException]
     * if invalid. Returns the peer initialized using the packet source address and the public key
     * from [BinMemberAuthenticationPayload], and the payload excluding
     * [BinMemberAuthenticationPayload] and the signature.
     *
     * @throws PacketDecodingException If the packet is authenticated and the signature is invalid.
     */
    @Throws(PacketDecodingException::class)
    private fun getAuthPayload(): Pair<Peer, ByteArray> {
        // prefix + message type
        val authOffset = PREFIX_SIZE + 1
        val (auth, authSize) = BinMemberAuthenticationPayload.deserialize(data, authOffset)
        val publicKey = try {
            defaultCryptoProvider.keyFromPublicBin(auth.publicKey)
        } catch (e: IllegalArgumentException) {
            throw PacketDecodingException("Incoming packet has an invalid public key", e)
        }
        val signatureOffset = data.size - publicKey.getSignatureLength()
        val signature = data.copyOfRange(signatureOffset, data.size)

        // Verify signature
        val message = data.copyOfRange(0, signatureOffset)
        val isValidSignature = publicKey.verify(signature, message)
        if (!isValidSignature)
            throw PacketDecodingException("Incoming packet has an invalid signature")

        // Return the peer and remaining payloads
        val peer = Peer(publicKey, source)
        val remainder = data.copyOfRange(authOffset + authSize,
            data.size - publicKey.getSignatureLength())
        return Pair(peer, remainder)
    }

    companion object {
        /**
         * The service ID size in bytes.
         */
        const val SERVICE_ID_SIZE = 20

        /**
         * The prefix size in bytes.
         */
        private const val PREFIX_SIZE = SERVICE_ID_SIZE + 2
    }
}
