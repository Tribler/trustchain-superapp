package nl.tudelft.trustchain.valuetransfer.community

import android.graphics.Bitmap
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.messaging.*
import nl.tudelft.trustchain.common.valuetransfer.extensions.bytesToImage
import nl.tudelft.trustchain.common.valuetransfer.extensions.imageBytes

class ContactImagePayload constructor(
    val publicKey: PublicKey,
    val imageHash: String?,
    val image: Bitmap?,
) : Serializable {
    override fun serialize(): ByteArray {
        val imageBytes = image?.let { imageBytes(it) }
        return serializeVarLen(publicKey.keyToBin()) +
            serializeVarLen((imageHash ?: "NONE").toByteArray()) +
            serializeVarLen(imageBytes ?: "NONE".toByteArray())
    }

    companion object Deserializer : Deserializable<ContactImagePayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<ContactImagePayload, Int> {
            var localOffset = offset
            val (publicKey, publicKeySize) = deserializeVarLen(buffer, localOffset)
            localOffset += publicKeySize
            val (imageHash, imageHashSize) = deserializeVarLen(buffer, localOffset)
            localOffset += imageHashSize
            var (image, imageSize) = deserializeVarLen(buffer, localOffset)
            localOffset += imageSize

            return Pair(
                ContactImagePayload(
                    defaultCryptoProvider.keyFromPublicBin(publicKey),
                    if (String(imageHash) == "NONE") null else imageHash.toString(Charsets.UTF_8),
                    if (String(image) == "NONE") null else bytesToImage(image)
                ),
                localOffset - offset
            )
        }
    }
}
