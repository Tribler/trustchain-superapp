package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_contacts.view.*
import kotlinx.android.synthetic.main.item_contacts.view.ivIdenticon
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.peerchat.ui.contacts.ContactItem
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.util.generateIdenticon

class ContactsItemRenderer(
    private val onChatClick: (Contact) -> Unit,
) : ItemLayoutRenderer<ContactItem, View>(
    ContactItem::class.java
) {

    override fun bindView(item: ContactItem, view: View) = with(view) {

        tvContactName.text = item.contact.name

        val publicKeyString = item.contact.publicKey.toString()
        val input = publicKeyString.substring(20, publicKeyString.length).toByteArray()
        val color = getColorByHash(context, publicKeyString)
        val identicon = generateIdenticon(input, color, resources)
        ivIdenticon.setImageBitmap(identicon)

        setOnClickListener {
            onChatClick(item.contact)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_contacts
    }
}
