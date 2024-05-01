package nl.tudelft.trustchain.currencyii.payload
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.SERIALIZED_USHORT_SIZE
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeUInt
import nl.tudelft.ipv8.messaging.deserializeUShort
import nl.tudelft.ipv8.messaging.serializeUInt
import nl.tudelft.ipv8.messaging.serializeUShort
import nl.tudelft.trustchain.currencyii.sharedWallet.SWResponseSignatureBlockTD
import nl.tudelft.trustchain.currencyii.sharedWallet.SWSignatureAskBlockTD
import java.util.Date

class SignPayload(
    val DAOid: ByteArray,
    val mostRecentSWBlock: TrustChainBlock,
    val proposeBlockData: SWSignatureAskBlockTD,
    val signatures: List<SWResponseSignatureBlockTD>
) : Serializable {
    override fun serialize(): ByteArray {
        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();

        val daoIdSizeBytes = serializeUShort(DAOid.size)

        // serialize each property seperately for mostRecentSWBlock
        val trustBlockTypeBytes = mostRecentSWBlock.type.toByteArray()
        val trustBlockTypeSizeBytes = serializeUShort(trustBlockTypeBytes.size)

        val trustBlockRawTransactionSizeBytes = serializeUShort(mostRecentSWBlock.rawTransaction.size)
        val trustBlockPublicKeySizeBytes = serializeUShort(mostRecentSWBlock.publicKey.size)

        val sequenceBytes = serializeUInt(mostRecentSWBlock.sequenceNumber)
        val sequenceSizeBytes = serializeUShort(sequenceBytes.size)

        val linkPublicKeySizeBytes = serializeUShort(mostRecentSWBlock.linkPublicKey.size)

        val linkSequenceNumberBytes = serializeUInt(mostRecentSWBlock.linkSequenceNumber)
        val linkSequenceNumberSizeBytes = serializeUShort(linkSequenceNumberBytes.size)

        val prevHashSizeBytes = serializeUShort(mostRecentSWBlock.previousHash.size)
        val signatureSizeBytes = serializeUShort(mostRecentSWBlock.signature.size)

        val timeStampBytes = gson.toJson(mostRecentSWBlock.timestamp).toByteArray()
        val timeStampSizeBytes = serializeUShort(timeStampBytes.size)



        val proposeBlockDataJson = gson.toJson(proposeBlockData)
        val proposeBlockDataBytes = proposeBlockDataJson.toByteArray()
        val proposeBlockDataSizeBytes = serializeUShort(proposeBlockDataBytes.size)

        val signaturesJson = gson.toJson(signatures)
        val signaturesBytes = signaturesJson.toByteArray()
        val signaturesSizeBytes = serializeUShort(signaturesBytes.size)

        return daoIdSizeBytes + DAOid +
            trustBlockTypeSizeBytes + trustBlockTypeBytes +
            trustBlockRawTransactionSizeBytes + mostRecentSWBlock.rawTransaction +
            trustBlockPublicKeySizeBytes + mostRecentSWBlock.publicKey +
            sequenceSizeBytes + sequenceBytes +
            linkPublicKeySizeBytes + mostRecentSWBlock.linkPublicKey +
            linkSequenceNumberSizeBytes + linkSequenceNumberBytes +
            prevHashSizeBytes + mostRecentSWBlock.previousHash +
            signatureSizeBytes + mostRecentSWBlock.signature +
            timeStampSizeBytes + timeStampBytes +
            proposeBlockDataSizeBytes + proposeBlockDataBytes +
            signaturesSizeBytes + signaturesBytes
    }

    companion object Deserializer : Deserializable<SignPayload> {
        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();

        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<SignPayload, Int> {
            var localOffset = 0
            val daoIdSize = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val daoId =
                buffer.copyOfRange(
                    offset + localOffset,
                    offset + localOffset + daoIdSize
                )
            localOffset += daoIdSize

            val typeSize = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val type = buffer.copyOfRange(offset + localOffset, offset + localOffset + typeSize)
            localOffset += typeSize

            val rawTransactionSize = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val rawTransaction = buffer.copyOfRange(offset + localOffset, offset + localOffset + rawTransactionSize)
            localOffset += rawTransactionSize

            val publicKeySize = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val publicKey = buffer.copyOfRange(offset + localOffset, offset + localOffset + publicKeySize)
            localOffset += publicKeySize

            val sequenceNumberSize = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val sequenceNumber = buffer.copyOfRange(offset + localOffset, offset + localOffset + sequenceNumberSize)
            localOffset += sequenceNumberSize

            val linkPublicKeySize = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val linkPublicKey = buffer.copyOfRange(offset + localOffset, offset + localOffset + linkPublicKeySize)
            localOffset += linkPublicKeySize

            val linkSequenceNumberSize = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val linkSequenceNumber = buffer.copyOfRange(offset + localOffset, offset + localOffset + linkSequenceNumberSize)
            localOffset += linkSequenceNumberSize

            val previousHashSize = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val previousHash = buffer.copyOfRange(offset + localOffset, offset + localOffset + previousHashSize)
            localOffset += previousHashSize

            val signatureSize = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val signature = buffer.copyOfRange(offset + localOffset, offset + localOffset + signatureSize)
            localOffset += signatureSize

            val timestampSize = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val timestamp = buffer.copyOfRange(offset + localOffset, offset + localOffset + timestampSize)
            localOffset += timestampSize

            val date = gson.fromJson(timestamp.decodeToString(), Date::class.java)
            val mostRecentSWBlock = TrustChainBlock(type.decodeToString(), rawTransaction, publicKey, deserializeUInt(sequenceNumber),
                linkPublicKey, deserializeUInt(linkSequenceNumber), previousHash, signature, date)


            val proposeBlockDataSize = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val proposeBlockData =
                gson.fromJson(
                    buffer.copyOfRange(
                        offset + localOffset,
                        offset + localOffset + proposeBlockDataSize
                    ).decodeToString(),
                    SWSignatureAskBlockTD::class.java
                )
            localOffset += proposeBlockDataSize

            val itemType = object : TypeToken<List<SWResponseSignatureBlockTD>>() {}.type
            val signaturesSize = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val signatures =
                gson.fromJson<List<SWResponseSignatureBlockTD>>(
                    buffer.copyOfRange(
                        offset + localOffset,
                        offset + localOffset + signaturesSize
                    ).decodeToString(),
                    itemType
                )
            localOffset += signaturesSize

            return Pair(SignPayload(daoId, mostRecentSWBlock, proposeBlockData, signatures), localOffset)
        }
    }
}
