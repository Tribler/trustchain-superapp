package nl.tudelft.trustchain.peerchat.community

import mu.KotlinLogging
import nl.tudelft.ipv8.messaging.*

private val logger = KotlinLogging.logger {}

class MessagePayload constructor(
    val id: String,
    val message: String,
    val attachmentType: String,
    val attachmentSize: Long,
    val attachmentContent: ByteArray,
    val transactionHash: ByteArray?
) : Serializable {
    override fun serialize(): ByteArray {
        val thash = (transactionHash ?: "NONE".toByteArray())
        return serializeVarLen(id.toByteArray()) +
            serializeVarLen(message.toByteArray()) +
            serializeVarLen(attachmentType.toByteArray()) +
            serializeLong(attachmentSize) +
            serializeVarLen(attachmentContent) +
            serializeVarLen(thash)
    }

    companion object Deserializer : Deserializable<MessagePayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<MessagePayload, Int> {
            var localOffset = offset
            val (id, idSize) = deserializeVarLen(buffer, localOffset)
            localOffset += idSize
            val (message, messageSize) = deserializeVarLen(buffer, localOffset)
            localOffset += messageSize
            val (attachmentType, attachmentTypeSize) = deserializeVarLen(buffer, localOffset)
            localOffset += attachmentTypeSize
            val attachmentSize = deserializeLong(buffer, localOffset)
            localOffset += SERIALIZED_LONG_SIZE
            val (attachmentContent, attachmentContentSize) = deserializeVarLen(buffer, localOffset)
            localOffset += attachmentContentSize
            var (transactionHash, transactionHashSize) = deserializeVarLen(buffer, localOffset)
            localOffset += transactionHashSize

            logger.debug { "after deserialisation: ${String(transactionHash)}" }

            return Pair(
                MessagePayload(
                    id.toString(Charsets.UTF_8),
                    message.toString(Charsets.UTF_8),
                    attachmentType.toString(Charsets.UTF_8),
                    attachmentSize,
                    attachmentContent,
                    if (String(transactionHash) == "NONE") null else transactionHash
                ),
                localOffset - offset
            )
        }
    }
}
