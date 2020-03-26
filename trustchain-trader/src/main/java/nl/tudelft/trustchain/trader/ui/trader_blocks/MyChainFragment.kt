package nl.tudelft.trustchain.trader.ui.trader_blocks

class MyChainFragment : BlocksFragment() {
    override val isCrawlAllowed = false

    override fun getPublicKey(): ByteArray {
        return getTrustChainCommunity().myPeer.publicKey.keyToBin()
    }
}
