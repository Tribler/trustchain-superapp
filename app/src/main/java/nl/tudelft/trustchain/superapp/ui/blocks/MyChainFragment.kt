package nl.tudelft.trustchain.superapp.ui.blocks

class MyChainFragment : BlocksFragment() {
    override fun getPublicKey(): ByteArray {
        return getTrustChainCommunity().myPeer.publicKey.keyToBin()
    }
}
