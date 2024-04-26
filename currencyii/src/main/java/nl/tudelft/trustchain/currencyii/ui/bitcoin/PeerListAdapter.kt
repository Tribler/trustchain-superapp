package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import nl.tudelft.ipv8.Peer
import nl.tudelft.trustchain.currencyii.databinding.PeerRowDataBinding
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import java.text.SimpleDateFormat
import java.util.Calendar

class PeerListAdapter(
    private val context: BaseFragment,
    private var items: List<Peer>
) : BaseAdapter() {

    override fun getView(
        p0: Int,
        p1: View?,
        p2: ViewGroup?
    ): View {
        val binding =
            if (p1 != null) {
                PeerRowDataBinding.bind(p1)
            } else {
                PeerRowDataBinding.inflate(context.layoutInflater)
            }

        val view = binding.root

        val peer = items[p0]
        val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm")

        val ipv4 = binding.ipv4
        val lastRequest = binding.lastRequest;
        val lastResponse = binding.lastResponse;
        val publicKey = binding.publicKey
        var pingIcon = binding.pingIcon

        ipv4.text = peer.address.ip
        lastRequest.text = peer.lastRequest?.let { formatter.format(it) }
        lastResponse.text = peer.lastResponse?.let { formatter.format(it) }
        publicKey.text = peer.publicKey.toString()

        val color = if (peer.lastResponse != null && peer.lastResponse!!.time + 3000 >
            Calendar.getInstance().timeInMillis) Color.GREEN else Color.RED
        pingIcon.setColorFilter(color)

        return view
    }

    override fun getItem(p0: Int): Any {
        return items[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getCount(): Int {
        return items.size
    }

    fun updateItems(peers: List<Peer>) {
        this.items = peers
    }
}
