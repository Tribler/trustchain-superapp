package nl.tudelft.trustchain.payloadgenerator.ui.payload

import android.annotation.SuppressLint
import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_payload.view.*
import nl.tudelft.trustchain.payloadgenerator.R

class PayloadItemRenderer(
    private val onItemClick: (PayloadItem) -> Unit
) : ItemLayoutRenderer<PayloadItem, View>(
    PayloadItem::class.java) {
    @SuppressLint("SetTextI18n")
    override fun bindView(item: PayloadItem, view: View) = with(view) {
        txtPeerId.text = item.publicKey.toString()
        txtSending.text = item.primaryCurrency.toString() + " " + item.amount.toString()
        txtReceiving.text = item.secondaryCurrency.toString() + " " + item.price.toString()
        txtPayloadType.text = item.type.toString()
        setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_payload
    }
}
