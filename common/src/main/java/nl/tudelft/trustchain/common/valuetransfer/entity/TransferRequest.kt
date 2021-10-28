package nl.tudelft.trustchain.common.valuetransfer.entity

import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import org.json.JSONObject
import java.io.Serializable

data class TransferRequest(
    /**
     * The (optional) description of the transfer request
     */
    val description: String?,

    /**
     * The requested amount for transfer
     */
    val amount: Long,

    /**
     * The requestor of the request
     */
    val requestor: PublicKey,

    /**
     * The receiver of the request
     */
    val receiver: PublicKey
) : Serializable {

    fun serialize(): ByteArray = JSONObject().apply {
        put(TRANSFER_REQUEST_DESCRIPTION, description)
        put(TRANSFER_REQUEST_AMOUNT, amount)
        put(TRANSFER_REQUEST_REQUESTOR, requestor.keyToBin().toHex())
        put(TRANSFER_REQUEST_RECEIVER, receiver.keyToBin().toHex())
    }.toString().toByteArray()

    companion object : Deserializable<TransferRequest> {
        const val TRANSFER_REQUEST_DESCRIPTION = "description"
        const val TRANSFER_REQUEST_AMOUNT = "amount"
        const val TRANSFER_REQUEST_REQUESTOR = "requestor"
        const val TRANSFER_REQUEST_RECEIVER = "receiver"

        override fun deserialize(buffer: ByteArray, offset: Int): Pair<TransferRequest, Int> {
            val offsetBuffer = buffer.copyOfRange(offset, buffer.size)
            val json = JSONObject(offsetBuffer.decodeToString())
            return Pair(
                TransferRequest(
                    json.getString(TRANSFER_REQUEST_DESCRIPTION),
                    json.getLong(TRANSFER_REQUEST_AMOUNT),
                    defaultCryptoProvider.keyFromPublicBin(
                        json.getString(TRANSFER_REQUEST_REQUESTOR).hexToBytes()
                    ),
                    defaultCryptoProvider.keyFromPublicBin(
                        json.getString(TRANSFER_REQUEST_RECEIVER).hexToBytes()
                    )
                ),
                0
            )
        }
    }
}
