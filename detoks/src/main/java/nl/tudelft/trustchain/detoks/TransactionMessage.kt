package nl.tudelft.trustchain.detoks

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

data class TransactionMessage(
    val amount: Int,
    val senderMID: String,
    val recipientMID: String
) : Serializable {

    override fun serialize(): ByteArray {
        val stream = ByteArrayOutputStream()
        val oos = ObjectOutputStream(stream)
        oos.writeInt(amount)
        oos.writeUTF(senderMID)
        oos.writeUTF(recipientMID)
        oos.flush()
        return stream.toByteArray()
    }

    companion object Deserializer : Deserializable<TransactionMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<TransactionMessage, Int> {
            val stream = ByteArrayInputStream(buffer, offset, buffer.size - offset)
            val ois = ObjectInputStream(stream)
            val amount = ois.readInt()
            val senderMID = ois.readUTF()
            val recipientMID = ois.readUTF()
            val message = TransactionMessage(amount, senderMID, recipientMID)
            return Pair(message, stream.available())
        }
    }

}
