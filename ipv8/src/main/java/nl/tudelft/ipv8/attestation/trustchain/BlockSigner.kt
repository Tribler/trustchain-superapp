package nl.tudelft.ipv8.attestation.trustchain

interface BlockSigner {
    /**
     * It is called whenever a proposal block targeted to us is received and the last
     * `TrustChainSettings.validationRange` blocks have been received and validated.
     * `TrustChainCommunity.createAgreementBlock` should be called to create an agreement block.
     */
    fun onSignatureRequest(block: TrustChainBlock)
}
