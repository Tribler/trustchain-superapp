package nl.tudelft.trustchain.liquidity.util

import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.common.util.TrustChainHelper

class TrustChainInteractor(private val trustChainCommunity: TrustChainCommunity) {
    private val helper: TrustChainHelper by lazy {
        TrustChainHelper(trustChainCommunity)
    }
    private val publicKey: ByteArray = helper.getMyPublicKey()

    public fun createProposalBlock(message: String) {
        helper.createProposalBlock(message, publicKey)
    }
}
