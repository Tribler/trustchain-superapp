package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_contacts.view.*
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.valuetransfer.util.getFirstLettersFromString
import nl.tudelft.trustchain.peerchat.ui.contacts.ContactItem
import nl.tudelft.trustchain.valuetransfer.R

class ContactsItemRenderer(
    private val onChatClick: (Contact) -> Unit,
) : ItemLayoutRenderer<ContactItem, View>(
    ContactItem::class.java
) {

    override fun bindView(item: ContactItem, view: View) = with(view) {

        tvContactName.text = item.contact.name
        tvContactInitials.text = getFirstLettersFromString(if(!item.contact.name.isNullOrEmpty()) item.contact.name else "",2)

        val color = getColorByHash(context, item.contact.publicKey.toString())
        val newColor = Color.argb(100, Color.red(color), Color.green(color), Color.blue(color))
        ivContactNew.backgroundTintList = ColorStateList.valueOf(newColor)
        ivContactNew.setBackgroundResource(R.drawable.circle_stroked)

        setOnClickListener {
            onChatClick(item.contact)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_contacts
    }
}
