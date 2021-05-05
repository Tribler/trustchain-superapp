package nl.tudelft.trustchain.ssi.ui.sent

import android.annotation.SuppressLint
import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_sent_attestation.view.*
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.ssi.R

class SentItemRenderer(
    private val onRevoke: (SentItem) -> Unit,
    private val onItemLongClick: (SentItem) -> Unit,
) : ItemLayoutRenderer<SentItem, View>(
    SentItem::class.java
) {

    @SuppressLint("SetTextI18n")
    override fun bindView(item: SentItem, view: View) = with(view) {
        val it = item.attestation
        publicKey.text = it.publicKey.keyToHash().toHex()
        attributeName.text = it.attributeName
        metadata.text = String(it.metadata.serializedMetadata)

        revokeButton.setOnClickListener {
            onRevoke(item)
        }

        setOnClickListener {
            onItemLongClick(item)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_sent_attestation
    }
}
