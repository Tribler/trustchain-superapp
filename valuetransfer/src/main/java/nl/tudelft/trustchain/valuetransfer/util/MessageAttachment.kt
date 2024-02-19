package nl.tudelft.trustchain.valuetransfer.util

import android.content.Context
import nl.tudelft.ipv8.util.toHex
import java.io.File

@OptIn(ExperimentalUnsignedTypes::class)
data class MessageAttachment constructor(
    /**
     * The type of the attachment. Currently, only "image" is supported.
     */
    val type: String,
    /**
     * The size of the attachment in bytes.
     */
    val size: Long,
    /**
     * The hash of the attachment that can be used for retrieving its data.
     */
    val content: ByteArray
) {
    fun getFile(context: Context): File {
        return getFile(context, content.toHex())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessageAttachment

        if (type != other.type) return false
        if (size != other.size) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }

    companion object {
        const val TYPE_IMAGE = "image"
        const val TYPE_FILE = "file"
        const val TYPE_CONTACT = "contact"
        const val TYPE_LOCATION = "location"
        const val TYPE_IDENTITY_ATTRIBUTE = "identity_attribute"
        const val TYPE_IDENTITY_UPDATED = "identity_updated"
        const val TYPE_TRANSFER_REQUEST = "transfer_request"

        fun getFile(
            context: Context,
            id: String
        ): File {
            val path = "" + context.filesDir + "/" + id
            return File(path)
        }
    }
}
