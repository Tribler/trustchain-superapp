package nl.tudelft.trustchain.peerchat.entity

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
    fun serialize(): ByteArray = JSONObject().apply {
        put(PUBLIC_KEY, publicKey.keyToBin().toHex())
        put(IMAGE_HASH, imageHash)
        put(IMAGE, image?.let { imageBytes(it) })
    }.toString().toByteArray()
//
//    val encodedImage = image?.let { imageBytes(it) }
////        val encodedImage = if (image != null) {
////            imageToBytes(image)?.let { encodeImage(it) }
////        } else null

    companion object : Deserializable<ContactImage> {
        private const val PUBLIC_KEY = "public_key"
        private const val IMAGE_HASH = "image_hash"
        private const val IMAGE = "image"

        override fun deserialize(buffer: ByteArray, offset: Int): Pair<ContactImage, Int> {
            val offsetBuffer = buffer.copyOfRange(0, buffer.size)
            val json = JSONObject(offsetBuffer.decodeToString())
            val imageEncoded = json.getString(IMAGE)
//            val image = if (imageEncoded.isNotBlank()) bytesToImage(decodeImage(imageEncoded)) else null

            return Pair(ContactImage(
                defaultCryptoProvider.keyFromPublicBin(json.getString(PUBLIC_KEY).hexToBytes()),
                json.getString(IMAGE_HASH),
                if (imageEncoded.isNotBlank()) decodeImage(imageEncoded) else null
            ), 0)
        }

//        fun encodeImage(bytes: ByteArray): String {
//            return Base64.encodeToString(bytes, Base64.DEFAULT)
//        }
//
//        fun decodeImage(encodedString: String): ByteArray {
//            return Base64.decode(encodedString, Base64.DEFAULT)
//        }

//        fun encodeImage(bitmap: Bitmap): String? {
//            var encodedString: String? = null
//            val baos = ByteArrayOutputStream()
//            try {
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
//                val bytes = baos.toByteArray()
//                encodedString = Base64.encodeToString(bytes, Base64.DEFAULT)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//            return encodedString
//        }

//        fun imageToBytes(bitmap: Bitmap): ByteArray? {
//            val baos = ByteArrayOutputStream()
//
//            return try {
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
//                baos.toByteArray()
//            } catch(e: Exception) {
//                e.printStackTrace()
//                null
//            }
//        }

//        fun bytesToImage(bytes: ByteArray): Bitmap? {
//            return try {
//                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//            } catch (e: Exception) {
//                e.printStackTrace()
//                null
//            }
//        }

//        fun decodeImage(decoded: String): Bitmap {
//            val bytes = Base64.decode(decoded, Base64.DEFAULT)
//            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//        }
    }
}
