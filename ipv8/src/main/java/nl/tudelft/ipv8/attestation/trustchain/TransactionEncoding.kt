package nl.tudelft.ipv8.attestation.trustchain

import java.math.BigInteger

/**
 * Serializes and deserializes transactions using "a" format compatible with legacy
 * py-ipv8 implementation.
 */
object TransactionEncoding {
    private const val VERSION_A = 'a'

    private const val ASCII_DIGITS_START = 48
    private const val ASCII_DIGITS_END = 57

    /*
     * Type prefixes
     */
    private const val TYPE_INT = "i"
    private const val TYPE_LONG = "J"
    private const val TYPE_FLOAT = "f"
    private const val TYPE_STRING = "s"
    private const val TYPE_BYTES = "b"
    private const val TYPE_LIST = "l"
    private const val TYPE_SET = "L"
    private const val TYPE_TUPLE = "t"
    private const val TYPE_DICTIONARY = "d"
    private const val TYPE_NONE = "n"
    private const val TYPE_TRUE = "T"
    private const val TYPE_FALSE = "F"

    /**
     * Encodes data into version "a" binary stream.
     */
    fun encode(value: Any?): ByteArray {
        val encoded = encodeValue(value)
        return "$VERSION_A$encoded".toByteArray(Charsets.UTF_8)
    }

    @Suppress("UNCHECKED_CAST")
    private fun encodeValue(value: Any?): String {
        return when (value) {
            is Int -> encodeInt(value)
            is BigInteger -> encodeInt(value)
            is Long -> encodeLong(value)
            is String -> encodeString(value)
            is Float -> encodeFloat(value)
            is List<*> -> encodeList(value)
            is Set<*> -> encodeSet(value)
            is Map<*, *> -> encodeDictionary(value as Map<Any?, Any?>)
            is Boolean -> encodeBoolean(value)
            null -> encodeNone()
            else -> throw TransactionSerializationException("Unsupported value type")
        }
    }

    /**
     * Decodes data from version "a" binary stream. Returns the pair containg the offset and
     * the decoded value.
     */
    fun decode(buffer: ByteArray, offset: Int = 0): Pair<Int, Any?> {
        val version = buffer[offset]
        if (version.toChar() == VERSION_A) {
            return decodeValue(buffer, offset + 1)
        } else {
            throw TransactionSerializationException("Unknown version found: $version")
        }
    }

    private fun decodeValue(buffer: ByteArray, offset: Int): Pair<Int, Any?> {
        var index = offset
        while (buffer[index] in ASCII_DIGITS_START..ASCII_DIGITS_END) {
            index++
        }
        val count = buffer.copyOfRange(offset, index).toString(Charsets.UTF_8).toInt()

        return when (val type = buffer[index].toChar().toString()) {
            TYPE_INT -> decodeInt(buffer, index + 1, count)
            TYPE_LONG -> decodeLong(buffer, index + 1, count)
            TYPE_STRING, TYPE_BYTES -> decodeString(buffer, index + 1, count)
            TYPE_FLOAT -> decodeFloat(buffer, index + 1, count)
            TYPE_LIST, TYPE_TUPLE -> decodeList(buffer, index + 1, count)
            TYPE_SET -> decodeSet(buffer, index + 1, count)
            TYPE_DICTIONARY -> decodeDictionary(buffer, index + 1, count)
            TYPE_TRUE -> decodeTrue(index + 1)
            TYPE_FALSE -> decodeFalse(index + 1)
            TYPE_NONE -> decodeNone(index + 1)
            else -> throw IllegalArgumentException("Unknown value type: $type")
        }
    }

    private fun encodeInt(value: Int): String {
        val str = value.toString()
        return "" + str.length + TYPE_INT + str
    }

    private fun encodeInt(value: BigInteger): String {
        val str = value.toString()
        return "" + str.length + TYPE_INT + str
    }

    private fun decodeInt(buffer: ByteArray, offset: Int, count: Int): Pair<Int, BigInteger> {
        val value = buffer.copyOfRange(offset, offset + count)
            .toString(Charsets.UTF_8)
            .toBigInteger()
        return Pair(offset + count, value)
    }

    private fun encodeLong(value: Long): String {
        val str = value.toString()
        return "" + str.length + TYPE_LONG + str
    }

    private fun decodeLong(buffer: ByteArray, offset: Int, count: Int): Pair<Int, Long> {
        val value = buffer.copyOfRange(offset, offset + count)
            .toString(Charsets.UTF_8)
            .toLong()
        return Pair(offset + count, value)
    }

    private fun encodeFloat(value: Float): String {
        val str = value.toString()
        return "" + str.length + TYPE_FLOAT + str
    }

    private fun decodeFloat(buffer: ByteArray, offset: Int, count: Int): Pair<Int, Float> {
        val value = buffer.copyOfRange(offset, offset + count)
            .toString(Charsets.UTF_8)
            .toFloat()
        return Pair(offset + count, value)
    }

    private fun encodeString(value: String): String {
        return "" + value.length + TYPE_STRING + value
    }

    private fun decodeString(buffer: ByteArray, offset: Int, count: Int): Pair<Int, String> {
        val value = buffer.copyOfRange(offset, offset + count)
            .toString(Charsets.UTF_8)
        return Pair(offset + count, value)
    }

    private fun encodeList(values: List<Any?>): String {
        val encoded = StringBuilder()
        encoded.append(values.size)
        encoded.append(TYPE_LIST)

        for (value in values) {
            encoded.append(encodeValue(value))
        }

        return encoded.toString()
    }

    private fun decodeList(buffer: ByteArray, offset: Int, count: Int): Pair<Int, List<Any?>> {
        val container = mutableListOf<Any?>()
        var localOffset = offset

        for (i in 0 until count) {
            val (newOffset, value) = decodeValue(buffer, localOffset)
            localOffset = newOffset

            container += value
        }

        return Pair(localOffset, container)
    }

    private fun encodeSet(values: Set<Any?>): String {
        val encoded = StringBuilder()
        encoded.append(values.size)
        encoded.append(TYPE_SET)

        for (value in values) {
            encoded.append(encodeValue(value))
        }

        return encoded.toString()
    }

    private fun decodeSet(buffer: ByteArray, offset: Int, count: Int): Pair<Int, Set<Any?>> {
        val container = mutableSetOf<Any?>()
        var localOffset = offset

        for (i in 0 until count) {
            val (newOffset, value) = decodeValue(buffer, localOffset)
            localOffset = newOffset

            container += value
        }

        return Pair(localOffset, container)
    }

    private fun encodeDictionary(values: Map<Any?, Any?>): String {
        val encoded = StringBuilder()
        encoded.append(values.size)
        encoded.append(TYPE_DICTIONARY)

        for ((key, value) in values) {
            encoded.append(encodeValue(key))
            encoded.append(encodeValue(value))
        }

        return encoded.toString()
    }

    private fun decodeDictionary(buffer: ByteArray, offset: Int, count: Int): Pair<Int, Map<Any?, Any?>> {
        val container = mutableMapOf<Any?, Any?>()
        var localOffset = offset

        for (i in 0 until count) {
            val (keyOffset, key) = decodeValue(buffer, localOffset)
            localOffset = keyOffset

            val (valueOffset, value) = decodeValue(buffer, localOffset)
            localOffset = valueOffset

            container[key] = value
        }

        return Pair(localOffset, container)
    }

    private fun encodeNone(): String {
        return "0$TYPE_NONE"
    }

    private fun decodeNone(offset: Int): Pair<Int, Any?> {
        return Pair(offset, null)
    }

    private fun encodeBoolean(value: Boolean): String {
        return "0" + if (value) TYPE_TRUE else TYPE_FALSE
    }

    private fun decodeTrue(offset: Int): Pair<Int, Boolean> {
        return Pair(offset, true)
    }

    private fun decodeFalse(offset: Int): Pair<Int, Boolean> {
        return Pair(offset, false)
    }
}

class TransactionSerializationException(message: String) : Exception(message)
