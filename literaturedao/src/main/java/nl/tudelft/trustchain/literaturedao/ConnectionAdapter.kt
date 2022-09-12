package nl.tudelft.trustchain.literaturedao


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import nl.tudelft.ipv8.Peer
import kotlin.math.roundToInt

class ConnectionAdapter(val items: List<Peer>) :
    RecyclerView.Adapter<ConnectionAdapter.ViewHolder>() {

    /**
     * Inflates the item views which is designed in xml layout file
     *
     * create a new
     * {@link ViewHolder} and initializes some private fields to be used by RecyclerView.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.fragment_connection_item,
                parent,
                false
            )
        )
    }

    /**
     * Binds each item in the ArrayList to a view
     *
     * Called when RecyclerView needs a new {@link ViewHolder} of the given type to represent
     * an item.
     *
     * This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type. You can either create a new View manually or inflate it from an XML
     * layout file.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = items.get(position)
        holder.connectionFragmentId.text = item.mid;
        val avgPing = item.getAveragePing()

        val avgPingStr =
            if (!avgPing.isNaN()) "" + (avgPing * 1000).roundToInt() + " ms" else "? ms"

        holder.connectionFragmentMS.text = avgPingStr;

    }


    /**
     * Gets the number of items in the list
     */
    override fun getItemCount(): Int {
        return items.size
    }

    /**
     * A ViewHolder describes an item view and metadata about its place within the RecyclerView.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Holds the TextView that will add each item to
        val connectionFragmentId: TextView = view.findViewById<TextView>(R.id.connection_id)
        val connectionFragmentMS: TextView = view.findViewById<TextView>(R.id.connection_ms)
    }

    fun refresh() {
        notifyDataSetChanged()
    }
}


