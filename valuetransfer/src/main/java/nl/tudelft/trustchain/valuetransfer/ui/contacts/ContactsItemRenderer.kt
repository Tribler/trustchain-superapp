package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_contacts.view.*
import kotlinx.android.synthetic.main.item_contacts.view.ivIdenticon
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.util.generateIdenticon

class ContactsItemRenderer(
    private val onChatClick: (Contact) -> Unit,
) : ItemLayoutRenderer<ChatItem, View>(
    ChatItem::class.java
) {

    override fun bindView(item: ChatItem, view: View) = with(view) {

        tvContactName.text = item.contact.name

        item.contact.publicKey.toString().let { publicKeyString ->
            generateIdenticon(
                publicKeyString.substring(20, publicKeyString.length).toByteArray(),
                getColorByHash(context, publicKeyString),
                resources
            ).let {
                ivIdenticon.setImageBitmap(it)
            }
        }

        setOnClickListener {
            onChatClick(item.contact)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_contacts
    }
}
