package nl.tudelft.trustchain.payloadgenerator.ui.blocks

import nl.tudelft.trustchain.payloadgenerator.ui.blocks.BlocksFragment

class MyChainFragment : BlocksFragment() {
    override val isCrawlAllowed = false

    override fun getPublicKey(): ByteArray {
        return getTrustChainCommunity().myPeer.publicKey.keyToBin()
    }
}
