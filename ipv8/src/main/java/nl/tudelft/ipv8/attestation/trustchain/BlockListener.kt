package nl.tudelft.ipv8.attestation.trustchain

/**
 * This class defines a listener for TrustChain blocks with a specific type.
 */
interface BlockListener {
    /**
     * Method to indicate whether this listener wants a specific block signed or not.
     */
    fun shouldSign(block: TrustChainBlock): Boolean

    /**
     * This method is called when a listener receives a block that matches the BLOCK_CLASS.
     */
    fun onBlockReceived(block: TrustChainBlock)
}
