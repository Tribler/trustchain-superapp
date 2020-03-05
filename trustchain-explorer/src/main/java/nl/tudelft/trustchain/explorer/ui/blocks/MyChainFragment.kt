package nl.tudelft.trustchain.explorer.ui.blocks

class MyChainFragment : BlocksFragment() {
    override val isCrawlAllowed = false

    override fun getPublicKey(): ByteArray {
        return getTrustChainCommunity().myPeer.publicKey.keyToBin()
    }
}
