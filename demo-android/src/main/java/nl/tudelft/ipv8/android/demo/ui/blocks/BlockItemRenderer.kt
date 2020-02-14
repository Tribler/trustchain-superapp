package nl.tudelft.ipv8.android.demo.ui.blocks

import android.view.View
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_block.view.*
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.util.toHex

class BlockItemRenderer(
    private val onExpandClick: (BlockItem) -> Unit,
    private val onSignClick: (BlockItem) -> Unit
) : ItemLayoutRenderer<BlockItem, View>(BlockItem::class.java) {
    override fun bindView(item: BlockItem, view: View) = with(view) {
        val block = item.block
        txtPublicKey.text = block.publicKey.toHex()
        txtLinkPublicKey.text = block.linkPublicKey.toHex()
        txtSequenceNumber.text = if (block.sequenceNumber > 0u)
            "seq: " + block.sequenceNumber else null
        txtLinkSequenceNumber.text = if (block.linkSequenceNumber > 0u)
            "seq: " + block.linkSequenceNumber else null

        txtExpandedPublicKey.text = block.publicKey.toHex()
        txtExpandedLinkPublicKey.text = block.linkPublicKey.toHex()
        txtPrevHash.text = block.previousHash.toHex()
        txtType.text = block.type
        txtTransaction.text = block.transaction["message"]?.toString()
        txtExpandedTransaction.text = block.transaction.toString()
        txtTimestamp.text = block.timestamp.toString()
        txtInsertTime.text = block.insertTime?.toString()
        txtBlockHash.text = block.calculateHash().toHex()
        txtSignature.text = block.signature.toHex()

        setOnClickListener {
            onExpandClick(item)
        }

        //txtBlockStatus.isVisible = item.isExpanded
        expandedItem.isVisible = item.isExpanded
        btnExpand.scaleY = if (item.isExpanded) -1f else 1f

        ownChainIndicator.setBackgroundColor(
            ChainColor.getColor(view.context, item.block.publicKey.toHex()))
        linkChainIndicator.setBackgroundColor(
            ChainColor.getColor(view.context, item.block.linkPublicKey.toHex()))

        signButton.isVisible = item.canSign
        signButton.setOnClickListener {
            onSignClick(item)
        }

        txtBlockStatus.isVisible = item.status != null

        when (item.status) {
            BlockItem.BlockStatus.WAITING_FOR_SIGNATURE -> {
                txtBlockStatus.text = context.getString(R.string.block_status, "Waiting for Signature")
                txtBlockStatus.setBackgroundColor(context.getColor(R.color.red))
            }
            BlockItem.BlockStatus.SELF_SIGNED -> {
                txtBlockStatus.text = context.getString(R.string.block_status, "Self-signed")
                txtBlockStatus.setBackgroundColor(context.getColor(R.color.green))
            }
            BlockItem.BlockStatus.SIGNED -> {
                txtBlockStatus.text = context.getString(R.string.block_status, "Signed")
                txtBlockStatus.setBackgroundColor(context.getColor(R.color.green))
            }
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_block
    }
}
