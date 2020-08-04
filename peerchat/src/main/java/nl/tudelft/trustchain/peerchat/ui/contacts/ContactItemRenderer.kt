package nl.tudelft.trustchain.peerchat.ui.contacts

import android.graphics.Typeface
import android.view.View
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_contact.view.*
import nl.tudelft.trustchain.peerchat.entity.Contact
import nl.tudelft.trustchain.peerchat.R
import java.text.SimpleDateFormat

class ContactItemRenderer(
    private val onItemClick: (Contact) -> Unit,
    private val onItemLongClick: (Contact) -> Unit
) : ItemLayoutRenderer<ContactItem, View>(
    ContactItem::class.java) {
    private val dateFormat = SimpleDateFormat.getDateTimeInstance()

    override fun bindView(item: ContactItem, view: View) = with(view) {
        txtName.text = item.contact.name
        // txtName.isVisible = item.contact.name.isNotEmpty()
        txtPeerId.text = item.contact.mid
        val lastMessageDate = item.lastMessage?.timestamp
        txtDate.isVisible = lastMessageDate != null
        txtDate.text = if (lastMessageDate != null) dateFormat.format(lastMessageDate) else null
        txtMessage.isVisible = item.lastMessage != null
        txtMessage.text = item.lastMessage?.message
        val textStyle = if (item.lastMessage?.read == false) Typeface.BOLD else Typeface.NORMAL
        txtMessage.setTypeface(null, textStyle)
        setOnClickListener {
            onItemClick(item.contact)
        }
        imgWifi.isVisible = item.isOnline
        imgBluetooth.isVisible = item.isBluetooth
        avatar.setUser(item.contact.mid, item.contact.name)
        setOnLongClickListener {
            onItemLongClick(item.contact)
            true
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_contact
    }
}
