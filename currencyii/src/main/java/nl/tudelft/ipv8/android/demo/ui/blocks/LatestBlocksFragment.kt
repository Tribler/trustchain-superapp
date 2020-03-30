package nl.tudelft.ipv8.android.demo.ui.blocks

import androidx.recyclerview.widget.LinearLayoutManager
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import kotlin.math.min

class LatestBlocksFragment : BlocksFragment() {
    override val isNewBlockAllowed = false

    override fun getBlocks(): List<TrustChainBlock> {
        val allBlocks = getTrustChainCommunity().database.getAllBlocks()
            .sortedByDescending { it.insertTime }
        return allBlocks.subList(0, min(1000, allBlocks.size))
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
