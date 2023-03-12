package nl.tudelft.trustchain.detoks_engine.manage_tokens

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import nl.tudelft.trustchain.detoks_engine.R

class ListAdapter<T>(private val data: ArrayList<T>, private val mapper: (t: T) -> String, private val onItemClick: (Int) -> Unit) :
    RecyclerView.Adapter<ListAdapter.ViewHolder>() {

    private var selectedPos: Int = RecyclerView.NO_POSITION

        class ViewHolder(view: View): RecyclerView.ViewHolder(view){
            val textView: TextView

            init {
                textView = view.findViewById(R.id.recycler_item)
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder.itemView as TextView).text = mapper(data[position])
        holder.itemView.isSelected = position == selectedPos
        holder.itemView.setOnClickListener { _ -> onItemClick(position) }
    }
}

