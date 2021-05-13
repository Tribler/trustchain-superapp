package nl.tudelft.trustchain.ssi.ui.sent

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_sent_attestation.view.*
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.ssi.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        val sdf = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
        val netDate = Date(it.signDate.toLong() * 1000)
        signDate.text = sdf.format(netDate)
        metadata.text = String(it.metadata.serializedMetadata)

        revokeButton.isActivated = !it.isRevoked
        revokeButton.isClickable = !it.isRevoked
        if (it.isRevoked) {
            revokeButton.setTextColor(Color.GRAY);
        } else {
            revokeButton.setOnClickListener {
                onRevoke(item)
            }
        }

        setOnClickListener {
            onItemLongClick(item)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_sent_attestation
    }
}
