package nl.tudelft.trustchain.peerchat

import android.view.View
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_contact.view.*
import nl.tudelft.ipv8.util.toHex
import java.text.SimpleDateFormat

class ContactItemRenderer(
    private val onItemClick: (Contact) -> Unit
) : ItemLayoutRenderer<ContactItem, View>(ContactItem::class.java) {
    private val dateFormat = SimpleDateFormat.getDateTimeInstance()

    override fun bindView(item: ContactItem, view: View) = with(view) {
        txtName.text = item.contact.name
        txtPeerId.text = item.contact.publicKey.keyToHash().toHex()
        val lastMessageDate = item.lastMessageDate
        txtDate.isVisible = lastMessageDate != null
        txtDate.text = if (lastMessageDate != null) dateFormat.format(lastMessageDate) else null
        txtMessage.isVisible = item.lastMessage != null
        txtMessage.text = item.lastMessage
        setOnClickListener {
            onItemClick(item.contact)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_contact
    }
}
