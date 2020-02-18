package nl.tudelft.ipv8.android.demo.ui.peers

import android.annotation.SuppressLint
import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_peer.view.*
import nl.tudelft.ipv8.android.demo.R
import java.util.*
import kotlin.math.roundToInt

class AddressItemRenderer(
    private val onItemClick: (AddressItem) -> Unit
) : ItemLayoutRenderer<AddressItem, View>(AddressItem::class.java) {
    @SuppressLint("SetTextI18n")
    override fun bindView(item: AddressItem, view: View) = with(view) {
        txtPeerId.text = "?"
        txtAddress.text = item.address.ip + ":" + item.address.port
        val lastRequest = item.contacted
        val lastResponse = item.discovered

        txtLastSent.text = if (lastRequest != null)
            "" + ((Date().time - lastRequest.time) / 1000.0).roundToInt() + " s" else "?"

        txtLastReceived.text = "" + ((Date().time - lastResponse.time) / 1000.0).roundToInt() + " s"

        txtAvgPing.text = "? ms"

        setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_peer
    }
}
