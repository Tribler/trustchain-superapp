package nl.tudelft.trustchain.valuetransfer.util

import android.graphics.Bitmap
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.valuetransfer.extensions.decodeImage
import nl.tudelft.trustchain.common.valuetransfer.extensions.imageBytes
import org.json.JSONObject
import java.io.Serializable

data class ContactImage(
    /**
     * The public key of the contact.
     */
    val publicKey: PublicKey,
    /**
     * Hash of image
     */
    val imageHash: String?,
    /**
     * Image of contact
     */
    val image: Bitmap?,
) : Serializable {
    fun serialize(): ByteArray =
        JSONObject().apply {
            put(PUBLIC_KEY, publicKey.keyToBin().toHex())
            put(IMAGE_HASH, imageHash)
            put(IMAGE, image?.let { imageBytes(it) })
        }.toString().toByteArray()

    companion object : Deserializable<ContactImage> {
        private const val PUBLIC_KEY = "public_key"
        private const val IMAGE_HASH = "image_hash"
        private const val IMAGE = "image"

        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<ContactImage, Int> {
            val offsetBuffer = buffer.copyOfRange(0, buffer.size)
            val json = JSONObject(offsetBuffer.decodeToString())
            val imageEncoded = json.getString(IMAGE)

            return Pair(
                ContactImage(
                    defaultCryptoProvider.keyFromPublicBin(json.getString(PUBLIC_KEY).hexToBytes()),
                    json.getString(IMAGE_HASH),
                    if (imageEncoded.isNotBlank()) decodeImage(imageEncoded) else null
                ),
                0
            )
        }
    }
}
