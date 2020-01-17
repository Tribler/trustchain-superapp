package nl.tudelft.ipv8.messaging.payload

import kotlin.experimental.and

fun createConnectionByte(connectionType: ConnectionType, advice: Boolean = false): Byte {
    var connectionByte: UByte = 0x00u
    if (connectionType.encoding.first) {
        connectionByte = connectionByte or 0x80.toUByte()
    }
    if (connectionType.encoding.second) {
        connectionByte = connectionByte or 0x40.toUByte()
    }
    if (advice) {
        connectionByte = connectionByte or 0x01.toUByte()
    }
    return connectionByte.toByte()
}

fun deserializeConnectionByte(byte: Byte): Pair<Boolean, ConnectionType> {
    val advice = (byte and 0x01) == 0x01.toByte()
    val bit0 = (byte.toUByte() and 0x80.toUByte()) != 0x00.toUByte()
    val bit1 = (byte.toUByte() and 0x40.toUByte()) != 0x00.toUByte()
    return Pair(advice, ConnectionType.decode(bit0, bit1))
}
