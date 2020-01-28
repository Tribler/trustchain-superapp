package nl.tudelft.ipv8.attestation.trustchain

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream


/**
 * Serializes and deserializes transactions using native Java primitives. Not that this is not
 * currently compatible with Python encoding implementation.
 */
object TransactionSerialization {
    fun deserialize(tx: ByteArray): TrustChainTransaction {
        try {
            val bis = ByteArrayInputStream(tx)
            return ObjectInputStream(bis).use { ins ->
                ins.readObject() as Map<*, *>
            }
        } catch (e: Exception) {
            throw TransactionSerializationException(e)
        }
    }

    fun serialize(tx: TrustChainTransaction): ByteArray {
        try {
            return ByteArrayOutputStream().use { bos ->
                val out = ObjectOutputStream(bos)
                out.writeObject(tx)
                out.flush()
                bos.toByteArray()
            }
        } catch (e: Exception) {
            throw TransactionSerializationException(e)
        }
    }
}

class TransactionSerializationException(cause: Exception?) : Exception(cause)
