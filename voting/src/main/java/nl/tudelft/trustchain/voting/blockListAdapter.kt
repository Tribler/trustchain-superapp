package nl.tudelft.trustchain.voting

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import org.json.JSONObject
import java.util.*

class blockListAdapter(private val myDataset: List<TrustChainBlock>) :

    RecyclerView.Adapter<blockListAdapter.MyViewHolder>() {

    var onItemClick: ((TrustChainBlock) -> Unit)? = null

    inner class MyViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView) {
        init {
            itemView.setOnClickListener {
                onItemClick?.invoke(myDataset[adapterPosition])
            }
        }

        val propTitle = cardView.findViewById<TextView>(R.id.propTitle)
        val propDate = cardView.findViewById<TextView>(R.id.propDate)
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
        holder.propTitle.text =
            JSONObject(myDataset[position].transaction["message"].toString()).get("VOTE_SUBJECT")
                .toString()

        val regex = Regex("^(.*?)GMT")
        val strippedText = regex.find(myDataset[position].timestamp.toString())?.value.toString()
        holder.propDate.text = strippedText.substring(0, strippedText.length - 3)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size
}
