package nl.tudelft.trustchain.valuetransfer.entity

import java.util.*

data class Attribute(
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
)
