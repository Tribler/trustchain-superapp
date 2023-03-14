package nl.tudelft.trustchain.detoks.adapters

import android.app.Activity
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import nl.tudelft.trustchain.detoks.R

class NotificationsListAdapter(private val context: Activity, private val notifications: List<String>) : ArrayAdapter<String>(context, R.layout.one_line_list_item, notifications) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val result = convertView ?: View.inflate(context, R.layout.one_line_list_item, null)

        val notificationLabel = result.findViewById<TextView>(R.id.title)
        notificationLabel.text = notifications[position]
        notificationLabel.setTypeface(null, Typeface.NORMAL)

        return result
    }
}
