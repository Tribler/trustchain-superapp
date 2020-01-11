package nl.tudelft.ipv8

import nl.tudelft.ipv8.messaging.*
import java.net.InetSocketAddress
import java.net.SocketAddress

/**
 * The pair of an IP address and a port.
 */
data class Address(
    val ip: String,
    val port: Int
) : Serializable {
    override fun serialize(): ByteArray {
        val parts = ip.split(".")
        val ipBytes = ByteArray(4)
        for (i in ipBytes.indices) {
            ipBytes[i] = parts[i].toInt().toChar().toByte()
        }
        return ipBytes + serializeUShort(port)
    }

    fun toSocketAddress(): SocketAddress {
        return InetSocketAddress(ip, port)
    }

    fun isEmpty(): Boolean {
        return ip == "0.0.0.0" && port == 0
    }

    companion object : Deserializable<Address> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<Address, Int> {
            var localOffset = 0
            val ip = "" +
                    buffer[offset + 0].toChar().toInt() + "." +
                    buffer[offset + 1].toChar().toInt() + "." +
                    buffer[offset + 2].toChar().toInt() + "." +
                    buffer[offset + 3].toChar().toInt()
            localOffset += 4
            val port = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            return Pair(Address(ip, port), localOffset)
        }

        const val SERIALIZED_SIZE = 6
    }
}
