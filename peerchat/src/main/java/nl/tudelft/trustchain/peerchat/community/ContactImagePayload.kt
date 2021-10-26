package nl.tudelft.trustchain.peerchat.community

import android.graphics.Bitmap
import android.util.Log
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.messaging.*
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.valuetransfer.extensions.bytesToImage
import nl.tudelft.trustchain.common.valuetransfer.extensions.imageBytes

class ContactImagePayload constructor(
    val publicKey: PublicKey,
    val imageHash: String?,
    val image: Bitmap?,
) : Serializable {
    override fun serialize(): ByteArray {
//        val imageBytes = if (image != null) ContactImage.imageToBytes(image) else null
        val imageBytes = image?.let { imageBytes(it) }
        return serializeVarLen(publicKey.keyToBin()) +
            serializeVarLen((imageHash ?: "NONE").toByteArray()) +
            serializeVarLen(imageBytes ?: "NONE".toByteArray())
    }

    companion object Deserializer : Deserializable<ContactImagePayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<ContactImagePayload, Int> {
            Log.d("VTLOG", "BEFORE DESERIALISATION ${buffer.toString()}")
            var localOffset = offset
            val (publicKey, publicKeySize) = deserializeVarLen(buffer, localOffset)
            localOffset += publicKeySize
            Log.d("VTLOG", "PUBLIC KEY: ${publicKey.toHex()}")
            val (imageHash, imageHashSize) = deserializeVarLen(buffer, localOffset)
            localOffset += imageHashSize
            var (image, imageSize) = deserializeVarLen(buffer, localOffset)
            localOffset += imageSize

            Log.d("VTLOG", "AFTER DESERIALISATION")

            return Pair(
                ContactImagePayload(
                    defaultCryptoProvider.keyFromPublicBin(publicKey),
                    if (String(imageHash) == "NONE") null else imageHash.toString(Charsets.UTF_8),
//                    if (String(image) == "NONE") null else ContactImage.bytesToImage(image),
                    if (String(image) == "NONE") null else bytesToImage(image)
                ),
                localOffset - offset
            )
        }
    }
}
