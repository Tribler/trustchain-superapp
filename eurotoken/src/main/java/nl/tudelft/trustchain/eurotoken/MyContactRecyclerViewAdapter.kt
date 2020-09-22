package nl.tudelft.trustchain.eurotoken

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import nl.tudelft.trustchain.eurotoken.dummy.DummyContent.DummyItem

/**
 * [RecyclerView.Adapter] that can display a [DummyItem].
 * TODO: Replace the implementation with code for your data type.
 */
class MyContactRecyclerViewAdapter(
    private val values: List<ContactItem>
) : RecyclerView.Adapter<MyContactRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_contact, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.avatarView.setUser(item.contact.publicKey, item.contact.name)
        holder.dateView.text = item.lastTransaction.timestamp.toString()
        holder.nameView.text = item.contact.name
        holder.pkView.text = item.contact.publicKey
        holder.contentView.text = (if (item.lastTransaction.outgoing) "Sent " else "Received ") +
            item.lastTransaction.amount.toString() +
            " ET" +
            (if (item.lastTransaction.outgoing) " to " else " from ") +
            item.contact.name
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarView: AvatarView = view.findViewById(R.id.avatar)
        val dateView: TextView = view.findViewById(R.id.txtDate)
        val nameView: TextView = view.findViewById(R.id.txtName)
        val pkView: TextView = view.findViewById(R.id.txtPeerId)
        val contentView: TextView = view.findViewById(R.id.txtContent)

        override fun toString(): String {
            return super.toString() + " '" + nameView.text + "'"
        }
    }
}
