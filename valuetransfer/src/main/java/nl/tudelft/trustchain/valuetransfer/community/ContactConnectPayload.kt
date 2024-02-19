package nl.tudelft.trustchain.valuetransfer.community

import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.messaging.*
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityInfo

class ContactConnectPayload(
    val sender: PublicKey,
    val identityInfo: IdentityInfo,
//    val name: String,
//    val verificationStatus: Boolean,
//    val type: ContactConnectType
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(sender.keyToBin()) +
            identityInfo.serialize()
//            serializeVarLen(name.toByteArray(Charsets.UTF_8)) +
//            serializeVarLen(verificationStatus.toString().toByteArray(Charsets.UTF_8)) +
//            serializeVarLen(type.toString().toByteArray(Charsets.UTF_8))
    }

    companion object Deserializer : Deserializable<ContactConnectPayload> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<ContactConnectPayload, Int> {
            var localOffset = offset
            val (sender, senderSize) = deserializeVarLen(buffer, localOffset)
            localOffset += senderSize
            val identityInfo = IdentityInfo.deserialize(buffer, localOffset)
//            val (name, nameSize) = deserializeVarLen(buffer, localOffset)
//            localOffset += nameSize
//            val (verificationStatus, verificationStatusSize) = deserializeVarLen(
//                buffer,
//                localOffset
//            )
//            localOffset += verificationStatusSize
//            val (type, typeSize) = deserializeVarLen(buffer, localOffset)
//            localOffset += typeSize

            return Pair(
                ContactConnectPayload(
                    defaultCryptoProvider.keyFromPublicBin(sender),
                    identityInfo.first,
//                    String(name, Charsets.UTF_8),
//                    String(verificationStatus, Charsets.UTF_8).toBoolean(),
//                    ContactConnectType.valueOf(String(type, Charsets.UTF_8))
                ),
                localOffset - offset
            )
        }
    }

//    enum class ContactConnectType {
//        INVITATION,
//        CONFIRMATION
//    }
}
