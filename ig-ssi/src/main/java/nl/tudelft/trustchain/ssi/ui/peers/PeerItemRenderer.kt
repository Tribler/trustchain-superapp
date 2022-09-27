package nl.tudelft.trustchain.ssi.ui.peers

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_peer.view.*
import nl.tudelft.trustchain.ssi.R
import java.util.*
import kotlin.math.roundToInt

class PeerItemRenderer(
    private val onItemClick: (PeerItem) -> Unit,
    private val onItemLongClick: (PeerItem) -> Boolean,
) : ItemLayoutRenderer<PeerItem, View>(
    PeerItem::class.java
) {
    @SuppressLint("SetTextI18n")
    override fun bindView(item: PeerItem, view: View) = with(view) {
        txtPeerId.text = item.peer.mid
        txtAddress.text = item.peer.address.toString()
        txtAddress.isVisible = !item.peer.address.isEmpty()
        txtBluetoothAddress.text = item.peer.bluetoothAddress?.toString()
        txtBluetoothAddress.isVisible = item.peer.bluetoothAddress != null
        val avgPing = item.peer.getAveragePing()
        val lastRequest = item.peer.lastRequest
        val lastResponse = item.peer.lastResponse

        txtLastSent.text = if (lastRequest != null)
            "" + ((Date().time - lastRequest.time) / 1000.0).roundToInt() + " s" else "?"

        txtLastReceived.text = if (lastResponse != null)
            "" + ((Date().time - lastResponse.time) / 1000.0).roundToInt() + " s" else "?"

        txtAvgPing.text =
            if (!avgPing.isNaN()) "" + (avgPing * 1000).roundToInt() + " ms" else "? ms"

        setOnClickListener {
            onItemClick(item)
        }
        setOnLongClickListener {
            onItemLongClick(item)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_peer
    }
}
