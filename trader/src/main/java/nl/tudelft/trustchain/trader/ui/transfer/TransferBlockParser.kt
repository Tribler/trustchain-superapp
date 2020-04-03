package nl.tudelft.trustchain.trader.ui.transfer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.util.TrustChainHelper
import java.util.*

class TransferBlockParser {
    val UNKNOWN_SEQ = 0u


    fun proposalToString(block: TrustChainBlock): String {

        val serializableBlock: SerializableBlock = SerializableBlock(
            block.publicKey.toHex(),
            block.sequenceNumber.toInt(),
            block.previousHash,
            block.signature,
            block.rawTransaction,
            block.timestamp.time
        )
        val json = Json(JsonConfiguration.Stable)
        return json.stringify(SerializableBlock.serializer(), serializableBlock)
    }

    fun stringToProposal(jsonString: String, trustChain: TrustChainHelper): TrustChainBlock {
        val json = Json(JsonConfiguration.Stable)
        val serializableBlock = json.parse(SerializableBlock.serializer(), jsonString)
        return TrustChainBlock("demo_tx_block", serializableBlock.transaction, serializableBlock.publicKey.hexToBytes(), serializableBlock.sequenceNumber.toUInt(),
            trustChain.getMyPublicKey(), UNKNOWN_SEQ, serializableBlock.previousHash, serializableBlock.signature, Date(serializableBlock.timeStamp))
    }
}
