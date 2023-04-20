package nl.tudelft.trustchain.detoks

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import nl.tudelft.ipv8.Peer

/**
 * Adapter class to create list items for the peer list
 */
class PeerAdapter (private val context: Activity,
                   private val peerArray: ArrayList<Peer>,
                   private val defaultPeer : Peer) : ArrayAdapter<Peer>(context, R.layout.list_item, peerArray) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.list_item, null, true)

        val peer = getItem(position)

        val item = rowView.findViewById(R.id.item_number) as TextView
        val peerId = rowView.findViewById(R.id.item_id) as TextView
        val address = rowView.findViewById(R.id.item_field1) as TextView

        item.text = "Peer: " + position.toString()
        peerId.text = "PK: " + peer.publicKey.toString()
        address.text = "Address: " + peer.address.toString()
        return rowView
    }

    /**
     * Retrieves item at position
     * @param position: position of item
     */
    override fun getItem(position: Int): Peer {
        return try {
            peerArray[position]
        } catch (e: java.lang.IndexOutOfBoundsException) {
            Log.d(
                "DeToksTransactionEngine",
                "Index out of bound for peerArray"
            )
            defaultPeer
        }
    }
}
