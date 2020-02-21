package nl.tudelft.ipv8.attestation.trustchain

/**
 * This class defines a listener for TrustChain blocks with a specific type.
 */
interface BlockListener {
    /**
     * It is called when a listener receives a block that matches the registered block type.
     */
    fun onBlockReceived(block: TrustChainBlock)

    /**
     * It is called whenever a proposal block targeted to use is received and the last
     * `TrustChainSettings.validationRange` blocks have been received and validated.
     * `TrustChainCommunity.createAgreementBlock` should be called to create an agreement block.
     */
    fun onSignatureRequest(block: TrustChainBlock)
}
