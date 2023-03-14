package nl.tudelft.trustchain.detoks.adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import nl.tudelft.trustchain.detoks.R

class VideosListAdapter(private val context: Activity, private val likesPerVideo: List<Pair<String, Int>>) : ArrayAdapter<Pair<String, Int>>(context, R.layout.two_line_list_item, likesPerVideo) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val result = convertView ?: View.inflate(context, R.layout.two_line_list_item, null)

        val videoIdLabel = result.findViewById<TextView>(R.id.title)
        videoIdLabel.text = likesPerVideo[position].first

        val numLikesLabel = result.findViewById<TextView>(R.id.subtitle)
        numLikesLabel.text = "Likes: " + likesPerVideo[position].second.toString()

        return result
    }
}
