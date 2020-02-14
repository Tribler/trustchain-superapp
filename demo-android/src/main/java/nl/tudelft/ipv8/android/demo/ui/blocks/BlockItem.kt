package nl.tudelft.ipv8.android.demo.ui.blocks

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock

data class BlockItem(
    val block: TrustChainBlock,
    var isExpanded: Boolean = false,
    val canSign: Boolean = false,
    val status: BlockStatus? = null
): Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is BlockItem && other.block.blockId == block.blockId
    }

    enum class BlockStatus {
        WAITING_FOR_SIGNATURE,
        SELF_SIGNED,
        SIGNED
    }
}
