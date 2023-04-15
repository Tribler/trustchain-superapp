package nl.tudelft.trustchain.detoks.adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import nl.tudelft.trustchain.detoks.R

class NetworkAdapter(private val context: Activity, private val peers: List<String>) : ArrayAdapter<String>(context, R.layout.one_line_list_item, peers) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val result = convertView ?: View.inflate(context, R.layout.one_line_list_item, null)

        val peerPublicKey = result.findViewById<TextView>(R.id.title)
        peerPublicKey.text = peers[position]

        return result
    }
}
