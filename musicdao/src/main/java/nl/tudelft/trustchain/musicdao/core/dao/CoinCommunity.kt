package nl.tudelft.trustchain.musicdao.core.dao

import nl.tudelft.ipv8.Community

@Suppress("UNCHECKED_CAST")
class CoinCommunity constructor(serviceId: String = "02313685c1912a141279f8248fc8db5899c5df5b") : Community() {
    override val serviceId = serviceId

    companion object {
        // Default maximum wait timeout for bitcoin transaction broadcasts in seconds
        const val DEFAULT_BITCOIN_MAX_TIMEOUT: Long = 10

        // Block type for join DAO blocks
        const val JOIN_BLOCK = "v1DAO_JOIN"

        // Block type for transfer funds (from a DAO)
        const val TRANSFER_FINAL_BLOCK = "v1DAO_TRANSFER_FINAL"

        // Block type for basic signature requests
        const val SIGNATURE_ASK_BLOCK = "v1DAO_ASK_SIGNATURE"

        // Block type for transfer funds signature requests
        const val TRANSFER_FUNDS_ASK_BLOCK = "v1DAO_TRANSFER_ASK_SIGNATURE"

        // Block type for responding to a signature request with a (should be valid) signature
        const val SIGNATURE_AGREEMENT_BLOCK = "v1DAO_SIGNATURE_AGREEMENT"

        // Block type for responding with a negative vote to a signature request with a signature
        const val SIGNATURE_AGREEMENT_NEGATIVE_BLOCK = "v1DAO_SIGNATURE_AGREEMENT_NEGATIVE"
    }
}
