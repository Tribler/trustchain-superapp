package nl.tudelft.peerchat.ui.peers

import android.annotation.SuppressLint
import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_peer.view.*
import nl.tudelft.peerchat.R
import java.text.DecimalFormat
import java.util.*
import kotlin.math.roundToInt

class PeerItemRenderer : ItemLayoutRenderer<PeerItem, View>(PeerItem::class.java) {
    @SuppressLint("SetTextI18n")
    override fun bindView(item: PeerItem, view: View) = with(view) {
        txtPeerId.text = item.peer.mid
        txtAddress.text = item.peer.address.ip + ":" + item.peer.address.port
        val avgPing = item.peer.getAveragePing()
        val lastPing = item.peer.pings.lastOrNull()
        val lastResponse = item.peer.lastResponse

        txtLastSent.text = "?"
        txtLastReceived.text = if (lastResponse != null)
            "" + ((Date().time - lastResponse.time)/1000.0).roundToInt() + " s" else "?"

        val decimalFormat = DecimalFormat()
        decimalFormat.maximumFractionDigits = 2
        txtAvgPing.text = decimalFormat.format(avgPing) + " s"
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_peer
    }
}