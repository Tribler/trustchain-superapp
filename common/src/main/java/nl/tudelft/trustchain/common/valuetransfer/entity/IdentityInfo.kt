package nl.tudelft.trustchain.common.valuetransfer.entity

import nl.tudelft.ipv8.messaging.Deserializable
import org.json.JSONObject
import java.io.Serializable

data class IdentityInfo(
    /**
     * The given names of the identity
     */
    val initials: String?,

    /**
     * The surname of the identity
     */
    val surname: String?,

    /**
     * The verification status of the identity
     */
    val isVerified: Boolean,

    /**
     * The hash of the image of the identity
     */
    val imageHash: String?
) : Serializable {
    fun serialize(): ByteArray = JSONObject().apply {
        put(IDENTITY_INFO_INITIALS, initials)
        put(IDENTITY_INFO_SURNAME, surname)
        put(IDENTITY_INFO_VERIFIED, isVerified)
        put(IDENTITY_INFO_IMAGE_HASH, imageHash)
    }.toString().toByteArray()

    companion object : Deserializable<IdentityInfo> {
        private const val IDENTITY_INFO_INITIALS = "identity_info_initials"
        private const val IDENTITY_INFO_SURNAME = "identity_info_surname"
        private const val IDENTITY_INFO_VERIFIED = "identity_info_verified"
        private const val IDENTITY_INFO_IMAGE_HASH = "identity_info_image_hash"

        override fun deserialize(buffer: ByteArray, offset: Int): Pair<IdentityInfo, Int> {
            val offsetBuffer = buffer.copyOfRange(offset, buffer.size)
            val json = JSONObject(offsetBuffer.decodeToString())

            return Pair(
                IdentityInfo(
                    json.optString(IDENTITY_INFO_INITIALS),
                    json.optString(IDENTITY_INFO_SURNAME),
                    json.getBoolean(IDENTITY_INFO_VERIFIED),
                    json.optString(IDENTITY_INFO_IMAGE_HASH) ?: null
                ),
                0
            )
        }
    }
}
