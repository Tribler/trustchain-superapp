package nl.tudelft.ipv8.android.demo.ui.blocks

import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_block.view.*
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.util.toHex

class BlockItemRenderer : ItemLayoutRenderer<BlockItem, View>(BlockItem::class.java) {
    override fun bindView(item: BlockItem, view: View) = with(view) {
        val block = item.block
        txtPublicKey.text = block.publicKey.toHex()
        txtLinkPublicKey.text = block.linkPublicKey.toHex()
        txtSequenceNumber.text = block.sequenceNumber.toString()
        txtLinkSequenceNumber.text = block.linkSequenceNumber.toString()
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_block
    }
}
