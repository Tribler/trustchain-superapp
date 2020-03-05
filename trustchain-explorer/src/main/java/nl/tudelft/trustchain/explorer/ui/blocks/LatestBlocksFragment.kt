package nl.tudelft.trustchain.explorer.ui.blocks

import androidx.recyclerview.widget.LinearLayoutManager
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock

class LatestBlocksFragment : BlocksFragment() {
    override val isNewBlockAllowed = false

    override fun getBlocks(): List<TrustChainBlock> {
        return getTrustChainCommunity().database.getRecentBlocks(1000)
            .sortedByDescending { it.insertTime }
    }

    override fun getPublicKey(): ByteArray {
        return getTrustChainCommunity().myPeer.publicKey.keyToBin()
    }

    override suspend fun updateView() {
        val layoutManager = binding.recyclerView.layoutManager as LinearLayoutManager
        val firstVisibleItem = layoutManager.findFirstCompletelyVisibleItemPosition()

        super.updateView()

        if (firstVisibleItem == 0) {
            binding.recyclerView.smoothScrollToPosition(0)
        }
    }
}
