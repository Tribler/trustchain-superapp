package nl.tudelft.ipv8.attestation.trustchain

/**
 * This class defines a listener for TrustChain blocks with a specific type.
 */
interface BlockListener {
    /**
     * It is called when a listener receives a block that matches the registered block type.
     */
    fun onBlockReceived(block: TrustChainBlock)
}
