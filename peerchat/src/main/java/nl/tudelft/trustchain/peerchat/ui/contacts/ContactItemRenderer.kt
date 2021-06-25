package nl.tudelft.trustchain.peerchat.ui.contacts

import android.graphics.Typeface
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_contact.view.*
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.peerchat.R
import java.text.SimpleDateFormat

class ContactItemRenderer(
    private val onItemClick: (Contact) -> Unit,
    private val onItemLongClick: (Contact) -> Unit,
    private val hideDetails: Boolean
) : ItemLayoutRenderer<ContactItem, View>(
    ContactItem::class.java
) {
    private val dateFormat = SimpleDateFormat.getDateTimeInstance()
    private var selectedItem: ContactItem? = null
    private var selectedContainer: View? = null

    override fun bindView(item: ContactItem, view: View) = with(view) {
        txtName.text = item.contact.name
        // txtName.isVisible = item.contact.name.isNotEmpty()
        txtPeerId.text = item.contact.mid
        avatar.setUser(item.contact.mid, item.contact.name)

        if (hideDetails) {
            txtDate.isVisible = false
            txtMessage.isVisible = false
            imgWifi.isVisible = false
            imgBluetooth.isVisible = false
            container.setBackgroundResource(R.drawable.bg_selectable_container)
            container.isSelected = selectedItem?.areItemsTheSame(item) ?: false
        } else {
            val lastMessageDate = item.lastMessage?.timestamp
            txtDate.isVisible = lastMessageDate != null
            txtDate.text = if (lastMessageDate != null) dateFormat.format(lastMessageDate) else null
            txtMessage.isVisible = item.lastMessage != null
            txtMessage.text = item.lastMessage?.message
            val textStyle = if (item.lastMessage?.read == false) Typeface.BOLD else Typeface.NORMAL
            txtMessage.setTypeface(null, textStyle)
            imgWifi.isVisible = item.isOnline
            imgBluetooth.isVisible = item.isBluetooth
        }

        setOnClickListener {
            if (selectedContainer != null && selectedItem != null &&
                !selectedItem!!.areItemsTheSame(item)) {
                selectedContainer!!.isSelected = false
            }

            selectedItem = item
            selectedContainer = container
            container.isSelected = true
            onItemClick(item.contact)
        }
        setOnLongClickListener {
            onItemLongClick(item.contact)
            true
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_contact
    }
}
