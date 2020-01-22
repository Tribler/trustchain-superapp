package nl.tudelft.ipv8.android.demo.ui.peers

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.Peer

class PeerItem(val peer: Peer): Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is PeerItem && other.peer.mid == peer.mid
    }

    override fun areContentsTheSame(other: Item): Boolean {
        return false
    }
}
