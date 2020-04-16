package nl.tudelft.trustchain.voting

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.trustchain.common.util.VotingHelper
import org.json.JSONObject

class blockListAdapter(
    private val myDataset: List<TrustChainBlock>,
    private val votingHelper: VotingHelper,
    private val myPublicKey: nl.tudelft.ipv8.keyvault.PublicKey
) :

    RecyclerView.Adapter<blockListAdapter.MyViewHolder>() {

    var onItemClick: ((TrustChainBlock) -> Unit)? = null

    inner class MyViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView) {
        init {
            itemView.setOnClickListener {
                onItemClick?.invoke(myDataset[adapterPosition])
            }
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        // create a new view
        val voteBlock = LayoutInflater.from(parent.context)
            .inflate(R.layout.vote_block, parent, false) as TextView

        // set the view's size, margins, paddings and layout parameters
        return MyViewHolder(voteBlock)
    }

    // Display vote proposition
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        if (votingHelper.castedByPeer(myDataset[position], myPublicKey) == Pair(0, 0))
            holder.textView.setTypeface(null, Typeface.BOLD)

        holder.textView.text =
            JSONObject(myDataset[position].transaction["message"].toString()).get("VOTE_SUBJECT")
                .toString()
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size
}
