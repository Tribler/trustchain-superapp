package nl.tudelft.ipv8.android.demo.ui.blocks

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock

data class BlockItem(
    val block: TrustChainBlock,
    var isExpanded: Boolean = false
): Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is BlockItem && other.block.blockId == block.blockId
    }
}
