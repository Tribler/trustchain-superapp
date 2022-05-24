package nl.tudelft.trustchain.literaturedao

import android.app.DownloadManager
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import nl.tudelft.trustchain.literaturedao.data_types.Literature
import androidx.core.net.toUri
import java.lang.Integer.min

class ItemAdapter(val items: MutableList<Literature>) :
    RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    /**
     * Inflates the item views which is designed in xml layout file
     *
     * create a new
     * {@link ViewHolder} and initializes some private fields to be used by RecyclerView.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.fragment_literature,
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
        Log.e("litdao", item.toString())

        var keywords: String  = "";
        var i = 0;

        if(!item.keywords.isEmpty()) {
            item.keywords.slice(0..min(13, item.keywords.size - 1)).forEach {
                keywords = keywords.plus(it.first.plus(", "));
                i++;
            }
        }

        holder.LiteratureFragmentTitle.text = item.title;
        holder.LiteratureFragmentDate.text = item.date;
        holder.LiteratureFragmentKeywords.text = keywords;

        holder.LiteratureFragmentHolder.setOnClickListener {
            if (item.localFileUri.startsWith("file:")) {
                holder.LiteratureFragmentKeywords.context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
            } else {
                var intent = Intent(Intent.ACTION_VIEW)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                intent.setDataAndType(item.localFileUri.toUri(), "application/pdf")
                intent = Intent.createChooser(intent, "Open File")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                holder.LiteratureFragmentKeywords.context.startActivity(intent)
            }
        }
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
        val LiteratureFragmentTitle: TextView = view.findViewById<TextView>(R.id.literature_fragment_title)
        val LiteratureFragmentKeywords: TextView = view.findViewById<TextView>(R.id.keywords)
        val LiteratureFragmentDate: TextView = view.findViewById<TextView>(R.id.date)
        val LiteratureFragmentHolder: LinearLayout = view.findViewById<LinearLayout>(R.id.lit_item)
    }

    fun refresh(){
        notifyDataSetChanged()
    }
}
