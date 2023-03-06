package nl.tudelft.trustchain.detoks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.util.toHex

class ProposalAdapter (private val mList: List<ProposalViewModel>, val confirmProposalOnClick: confirmProposalOnClick): RecyclerView.Adapter<ProposalAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.incoming_proposal_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val proposalSender = mList[position].block.publicKey.toHex()
        val proposalBlockType = mList[position].blockType
        val proposalMessage = mList[position].block.transaction["message"]
        holder.proposalSender.text = proposalSender
        holder.proposalBlockType.text = proposalBlockType
        holder.proposalMessage.text = proposalMessage.toString()
        holder.acceptProposalButton.setOnClickListener {
            println("alert: confirming proposal")
            confirmProposalOnClick.confirmProposalClick(mList[position].block, this)
        }

    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var proposalSender: TextView = itemView.findViewById(R.id.proposalSenderTextview)
        var proposalBlockType: TextView = itemView.findViewById(R.id.blockTypeTextView)
        var acceptProposalButton: Button = itemView.findViewById(R.id.confirmProposalButton)
        var proposalMessage: TextView = itemView.findViewById(R.id.proposalMessageTextView)
    }

}
