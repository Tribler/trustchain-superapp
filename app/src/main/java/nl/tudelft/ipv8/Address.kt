package nl.tudelft.ipv8

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeUShort
import nl.tudelft.ipv8.messaging.serializeUShort
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

    companion object : Deserializable<Address> {
        override fun deserialize(buffer: ByteArray, offset: Int): Address {
            val ip = "" +
                    buffer[offset + 0].toChar().toInt() + "." +
                    buffer[offset + 1].toChar().toInt() + "." +
                    buffer[offset + 2].toChar().toInt() + "." +
                    buffer[offset + 3].toChar().toInt()
            val port = deserializeUShort(buffer, offset + 4)
            return Address(ip, port)
        }

        const val SERIALIZED_SIZE = 6
    }
}
