package nl.tudelft.trustchain.detoks.adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import nl.tudelft.trustchain.detoks.R

class LikedListAdapter(private val context: Activity, private val likedVideos: List<String>) : ArrayAdapter<String>(context, R.layout.one_line_list_item, likedVideos) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val result = convertView ?: View.inflate(context, R.layout.one_line_list_item, null)

        val videoIdLabel = result.findViewById<TextView>(R.id.title)
        videoIdLabel.text = likedVideos[position]

        return result
    }
}
