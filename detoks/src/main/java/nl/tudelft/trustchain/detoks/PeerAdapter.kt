package nl.tudelft.trustchain.detoks

import android.content.Context
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.util.toHex

class PeerAdapter (private val mList: List<PeerViewModel>, val transactionOnClick: singleTransactionOnClick, val benchmarkOnClick: benchmark1000) : RecyclerView.Adapter<PeerAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.discovered_peer_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        println("alert: onbindviewholder")
        val peerViewModel = mList[position]
        holder.pkView.setText(peerViewModel.peer.publicKey.keyToBin().toHex())
        holder.pkView.setOnClickListener {
            println("Alert: works for pk view")
        }

        holder.singleTransactionButton.setOnClickListener {
            println("alert: in adapter")
            transactionOnClick.onClick(peerViewModel.peer)
        }

        holder.benchmarkButton.setOnClickListener {
            benchmarkOnClick.runBenchmark(peerViewModel.peer)
        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        var pkView : TextView = itemView.findViewById(R.id.pkTextview)
        var singleTransactionButton: Button = itemView.findViewById(R.id.initializeTransactionButton)
        var benchmarkButton : Button = itemView.findViewById(R.id.button1000)

    }
}
