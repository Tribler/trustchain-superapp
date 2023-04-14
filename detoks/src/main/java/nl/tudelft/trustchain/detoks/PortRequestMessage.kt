package nl.tudelft.trustchain.detoks

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

data class PortRequestMessage(val senderMid: String, val port: String) : Serializable {
    override fun serialize(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
        val data = hashMapOf(
            "sender_mid" to senderMid,
            "port" to port
        )

        objectOutputStream.writeObject(data)
        objectOutputStream.flush()

        return byteArrayOutputStream.toByteArray()
    }

    companion object Deserializer : Deserializable<PortRequestMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<PortRequestMessage, Int> {
            val byteArrayInputStream = ByteArrayInputStream(buffer, offset, buffer.size - offset)
            val objectInputStream = ObjectInputStream(byteArrayInputStream)
            val data = objectInputStream.readObject() as HashMap<*, *>
            val portRequestMessage = PortRequestMessage(
                data["sender_mid"] as String,
                data["port"] as String
            )

            // Calculate the length of the serialized data
            val length = portRequestMessage.serialize().size

            return Pair(portRequestMessage, length)
        }
    }
}
