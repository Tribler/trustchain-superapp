package nl.tudelft.trustchain.peerchat.community

import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen

class ContactImageRequestPayload(
    val sender: PublicKey
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(sender.keyToBin())
    }

    companion object Deserializer : Deserializable<ContactImageRequestPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<ContactImageRequestPayload, Int> {
            var localOffset = offset
            val (sender, senderSize) = deserializeVarLen(buffer, localOffset)
            localOffset += senderSize

            return Pair(
                ContactImageRequestPayload(
                    defaultCryptoProvider.keyFromPublicBin(sender)
                ),
                localOffset - offset
            )
        }
    }
}
