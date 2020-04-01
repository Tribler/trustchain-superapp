package nl.tudelft.trustchain.currencyii.ui.blocks

class MyChainFragment : BlocksFragment() {
    override fun getPublicKey(): ByteArray {
        return getTrustChainCommunity().myPeer.publicKey.keyToBin()
    }
}
