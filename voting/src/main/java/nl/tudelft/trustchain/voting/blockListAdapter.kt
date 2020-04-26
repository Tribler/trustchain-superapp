package nl.tudelft.trustchain.voting

import android.content.res.ColorStateList
import android.graphics.Color
import android.text.SpannableString
import android.text.format.DateFormat
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.trustchain.common.util.VotingHelper
import org.json.JSONObject

class blockListAdapter(
    private val myDataset: List<TrustChainBlock>,
    private val vh: VotingHelper
) :

    RecyclerView.Adapter<blockListAdapter.MyViewHolder>() {

    var onItemClick: ((TrustChainBlock) -> Unit)? = null

    inner class MyViewHolder(cardView: CardView) : RecyclerView.ViewHolder(cardView) {
        init {
            itemView.setOnClickListener {
                onItemClick?.invoke(myDataset[adapterPosition])
            }
        }

        val progressBar: ProgressBar = cardView.findViewById(R.id.progressBar3)
        val propTitle: TextView = cardView.findViewById(R.id.propTitle)
        val propDate: TextView = cardView.findViewById(R.id.propDate)
        val newIndicator: TextView = cardView.findViewById(R.id.newIndicator)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        // create a new view
        val voteBlock = LayoutInflater.from(parent.context)
            .inflate(R.layout.proposal_block, parent, false) as CardView

        // set the view's size, margins, paddings and layout parameters
        return MyViewHolder(voteBlock)
    }

    // Display vote proposition
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val proposalBlock = myDataset[position]

        holder.propTitle.text =
            JSONObject(proposalBlock.transaction["message"].toString()).get("VOTE_SUBJECT")
                .toString()

        holder.propDate.text = DateFormat.format("EEE MMM d HH:mm", proposalBlock.timestamp).toString()

        holder.propTitle.text = JSONObject(proposalBlock.transaction["message"].toString()).get("VOTE_SUBJECT")
            .toString()

        if (vh.castedByPeer(proposalBlock, vh.myPublicKey) == Pair(0, 0)) {
            holder.newIndicator.text = "New"
        }

        try {
            val bar = holder.progressBar
            var thresholdNotMade = false
            val progressValue = vh.getVoteProgressStatus(proposalBlock, 1)

            if (progressValue == -1) {
                bar.progress = 100
                thresholdNotMade = true
            } else {
                bar.progress = vh.getVoteProgressStatus(proposalBlock, 1)
            }

            if (bar.progress == 100 && !thresholdNotMade) {
                bar.progressTintList = ColorStateList.valueOf(Color.GREEN)
            } else if (bar.progress == 100 && thresholdNotMade) {
                bar.progressTintList = ColorStateList.valueOf(Color.RED)
            } else {
                bar.progressTintList = ColorStateList.valueOf(Color.RED)
            }
        } catch (e: Exception) {
            holder.progressBar.progress = 0
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size
}
