package nl.tudelft.trustchain.valuetransfer.entity

import nl.tudelft.ipv8.messaging.Deserializable
import org.json.JSONObject
import java.io.Serializable
import java.util.*

data class IdentityAttribute(
    /**
     * The unique attribute ID.
     */
    val id: String,

    /**
     * The attribute name.
     */
    var name: String,

    /**
     * The attribute value.
     */
    var value: String,

    /**
     * Attribute added on date.
     */
    val added: Date,

    /**
     * Attribute modified on date.
     */
    var modified: Date,
) : Serializable {

    fun serialize(): ByteArray {
        val json = JSONObject()
        json.put(IDENTITY_ATTRIBUTE_ID, id)
        json.put(IDENTITY_ATTRIBUTE_NAME, name)
        json.put(IDENTITY_ATTRIBUTE_VALUE, value)
        json.put(IDENTITY_ATTRIBUTE_ADDED, added)
        json.put(IDENTITY_ATTRIBUTE_MODIFIED, modified)

        return json.toString().toByteArray()
    }

    companion object : Deserializable<IdentityAttribute> {
        private const val IDENTITY_ATTRIBUTE_ID = "attribute_id"
        private const val IDENTITY_ATTRIBUTE_NAME = "attribute_name"
        private const val IDENTITY_ATTRIBUTE_VALUE = "attribute_value"
        private const val IDENTITY_ATTRIBUTE_ADDED = "attribute_added"
        private const val IDENTITY_ATTRIBUTE_MODIFIED = "attribute_modified"

        val IDENTITY_ATTRIBUTES = listOf(
            "Country",
            "City",
            "Home Address",
            "Zip Code",
            "Email",
            "Phone Number",
            "Mobile Number"
        )

        override fun deserialize(buffer: ByteArray, offset: Int): Pair<IdentityAttribute, Int> {
            val offsetBuffer = buffer.copyOfRange(offset, buffer.size)
            val json = JSONObject(offsetBuffer.decodeToString())
            val attributeID = json.getString(IDENTITY_ATTRIBUTE_ID)
            val attributeName = json.getString(IDENTITY_ATTRIBUTE_NAME)
            val attributeValue = json.getString(IDENTITY_ATTRIBUTE_VALUE)
            val attributeAdded = Date()
            val attributeModified = Date()
            val attribute = IdentityAttribute(
                attributeID,
                attributeName,
                attributeValue,
                attributeAdded,
                attributeModified
            )
            return Pair(attribute, 0)
        }
    }
}
