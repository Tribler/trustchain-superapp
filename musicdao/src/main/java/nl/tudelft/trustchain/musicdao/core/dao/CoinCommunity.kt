package nl.tudelft.trustchain.musicdao.core.dao

import android.app.Activity
import android.content.Context
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.musicdao.core.util.DAOCreateHelper
import nl.tudelft.trustchain.musicdao.core.util.DAOJoinHelper
import nl.tudelft.trustchain.musicdao.core.util.DAOTransferFundsHelper
import nl.tudelft.trustchain.musicdao.core.util.sharedWallet.SWJoinBlockTD
import nl.tudelft.trustchain.musicdao.core.util.sharedWallet.SWJoinBlockTransactionData
import nl.tudelft.trustchain.musicdao.core.util.sharedWallet.SWResponseNegativeSignatureBlockTD
import nl.tudelft.trustchain.musicdao.core.util.sharedWallet.SWResponseNegativeSignatureTransactionData
import nl.tudelft.trustchain.musicdao.core.util.sharedWallet.SWResponseSignatureBlockTD
import nl.tudelft.trustchain.musicdao.core.util.sharedWallet.SWResponseSignatureTransactionData
import nl.tudelft.trustchain.musicdao.core.util.sharedWallet.SWSignatureAskBlockTD
import nl.tudelft.trustchain.musicdao.core.util.sharedWallet.SWSignatureAskTransactionData
import nl.tudelft.trustchain.musicdao.core.util.sharedWallet.SWTransferDoneTransactionData
import nl.tudelft.trustchain.musicdao.core.util.sharedWallet.SWTransferFundsAskBlockTD
import nl.tudelft.trustchain.musicdao.core.util.sharedWallet.SWTransferFundsAskTransactionData

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
