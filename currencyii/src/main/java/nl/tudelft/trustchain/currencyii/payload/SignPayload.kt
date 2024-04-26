package nl.tudelft.trustchain.currencyii.payload
import nl.tudelft.ipv8.messaging.*
import nl.tudelft.trustchain.currencyii.sharedWallet.SWSignatureAskBlockTD
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.trustchain.currencyii.sharedWallet.SWResponseSignatureBlockTD

class SignPayload(
    val DAOid: ByteArray,
    val mostRecentSWBlock: TrustChainBlock,
    val proposeBlockData: SWSignatureAskBlockTD,
    val signatures: List<SWResponseSignatureBlockTD>
) : Serializable {
    override fun serialize(): ByteArray {
        val gson = Gson()

        val daoIdSizeBytes = serializeUShort(DAOid.size)

        val mostRecentSWBlockJson = gson.toJson(mostRecentSWBlock)
        val mostRecentSWBlockBytes = mostRecentSWBlockJson.toByteArray()
        val mostRecentSWBlockSizeBytes = serializeUShort(mostRecentSWBlockBytes.size)

        val proposeBlockDataJson = gson.toJson(proposeBlockData)
        val proposeBlockDataBytes = proposeBlockDataJson.toByteArray()
        val proposeBlockDataSizeBytes = serializeUShort(proposeBlockDataBytes.size)

        val signaturesJson = gson.toJson(signatures)
        val signaturesBytes = signaturesJson.toByteArray()
        val signaturesSizeBytes = serializeUShort(signaturesBytes.size)

        return daoIdSizeBytes + DAOid +
            mostRecentSWBlockSizeBytes + mostRecentSWBlockBytes +
            proposeBlockDataSizeBytes + proposeBlockDataBytes +
            signaturesSizeBytes + signaturesBytes
    }

    companion object Deserializer : Deserializable<SignPayload> {
        val gson = Gson()

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

            val mostRecentSWBlockSize = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val mostRecentSWBlock =
                gson.fromJson(
                    buffer.copyOfRange(
                        offset + localOffset,
                        offset + localOffset + mostRecentSWBlockSize
                    ).decodeToString(),
                    TrustChainBlock::class.java
                )
            localOffset += mostRecentSWBlockSize

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
