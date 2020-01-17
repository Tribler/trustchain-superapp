package nl.tudelft.ipv8.messaging

const val SERIALIZED_USHORT_SIZE = 2
const val SERIALIZED_ULONG_SIZE = 8

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
