package nl.tudelft.trustchain.detoks

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

data class TransactionMessage(
    val amount: Float,
    val senderMID: String,
    val recipientMID: String
) : Serializable {

    override fun serialize(): ByteArray {
        val stream = ByteArrayOutputStream()
        val oos = ObjectOutputStream(stream)
        oos.writeFloat(amount)
        oos.writeUTF(senderMID)
        oos.writeUTF(recipientMID)
        oos.flush()
        return stream.toByteArray()
    }

    companion object Deserializer : Deserializable<TransactionMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<TransactionMessage, Int> {
            val stream = ByteArrayInputStream(buffer, offset, buffer.size - offset)
            val ois = ObjectInputStream(stream)
            val amount = ois.readFloat()
            val senderMID = ois.readUTF()
            val recipientMID = ois.readUTF()
            val message = TransactionMessage(amount, senderMID, recipientMID)
            return Pair(message, stream.available())
        }
    }

}

data class TokenRequestMessage(
    val amount: Float,
    val senderMid: String,
    val recipientMid: String
) : Serializable {
    override fun serialize(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
        val data = hashMapOf(
            "amount" to amount,
            "sender_mid" to senderMid,
            "recipient_mid" to recipientMid
        )

        objectOutputStream.writeObject(data)
        objectOutputStream.flush()

        return byteArrayOutputStream.toByteArray()
    }

    companion object Deserializer : Deserializable<TokenRequestMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<TokenRequestMessage, Int> {
            val byteArrayInputStream = ByteArrayInputStream(buffer)
            val objectInputStream = ObjectInputStream(byteArrayInputStream)
            val data = objectInputStream.readObject() as HashMap<*, *>
            val tokenRequestMessage = TokenRequestMessage(
                data["amount"] as Float,
                data["sender_mid"] as String,
                data["recipient_mid"] as String
            )

            // Calculate the length of the serialized data
            val length = tokenRequestMessage.serialize().size

            return Pair(tokenRequestMessage, length)
        }
    }

}
