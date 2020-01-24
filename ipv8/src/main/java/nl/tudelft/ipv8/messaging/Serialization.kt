package nl.tudelft.ipv8.messaging

import java.nio.ByteBuffer

const val SERIALIZED_USHORT_SIZE = 2
const val SERIALIZED_UINT_SIZE = 4
const val SERIALIZED_ULONG_SIZE = 8
const val SERIALIZED_LONG_SIZE = 8

const val SERIALIZED_PUBLIC_KEY_SIZE = 74
const val HASH_SIZE = 32
const val SIGNATURE_SIZE = 64

interface Serializable {
    fun serialize(): ByteArray
}

interface Deserializable<T> {
    fun deserialize(buffer: ByteArray, offset: Int = 0): Pair<T, Int>
}

fun serializeBool(data: Boolean): ByteArray {
    val value = if (data) 1 else 0
    val array = ByteArray(1)
    array[0] = value.toByte()
    return array
}

fun deserializeBool(buffer: ByteArray, offset: Int = 0): Boolean {
    return buffer[offset].toInt() > 0
}

fun serializeUShort(value: Int): ByteArray {
    val bytes = ByteArray(SERIALIZED_USHORT_SIZE)
    bytes[1] = (value and 0xFF).toByte()
    bytes[0] = ((value shr 8) and 0xFF).toByte()
    return bytes
}

fun deserializeUShort(buffer: ByteArray, offset: Int = 0): Int {
    return (((buffer[offset].toInt() and 0xFF) shl 8) or (buffer[offset + 1].toInt() and 0xFF))
}

fun serializeUInt(value: UInt): ByteArray {
    val bytes = ByteArray(SERIALIZED_UINT_SIZE)
    for (i in 0 until SERIALIZED_UINT_SIZE) {
        bytes[i] = ((value shr ((7 - i) * 8)) and 0xFFu).toByte()
    }
    return bytes
}

fun deserializeUInt(buffer: ByteArray, offset: Int = 0): UInt {
    var result = 0u
    for (i in 0 until SERIALIZED_UINT_SIZE) {
        result = (result shl 8) or buffer[offset + i].toUInt()
    }
    return result
}

fun serializeULong(value: ULong): ByteArray {
    val bytes = ByteArray(SERIALIZED_ULONG_SIZE)
    for (i in 0 until SERIALIZED_ULONG_SIZE) {
        bytes[i] = ((value shr ((7 - i) * 8)) and 0xFFu).toByte()
    }
    return bytes
}

fun deserializeULong(buffer: ByteArray, offset: Int = 0): ULong {
    var result = 0uL
    for (i in 0 until SERIALIZED_ULONG_SIZE) {
        result = (result shl 8) or buffer[offset + i].toULong()
    }
    return result
}

fun serializeLong(value: Long): ByteArray {
    val buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
    buffer.putLong(value)
    return buffer.array()
}

fun deserializeLong(bytes: ByteArray, offset: Int = 0): Long {
    val buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
    buffer.put(bytes.copyOfRange(offset, offset + SERIALIZED_LONG_SIZE))
    buffer.flip()
    return buffer.long
}

fun serializeVarLen(bytes: ByteArray): ByteArray {
    return serializeUInt(bytes.size.toUInt()) + bytes
}

fun deserializeVarLen(buffer: ByteArray, offset: Int = 0): Pair<ByteArray, Int> {
    val len = deserializeUInt(buffer, offset).toInt()
    val payload = buffer.copyOfRange(offset + SERIALIZED_UINT_SIZE,
        offset + SERIALIZED_UINT_SIZE + len)
    return Pair(payload, SERIALIZED_UINT_SIZE + len)
}
