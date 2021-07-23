package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_contacts_chat.view.*
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.valuetransfer.util.getFirstLettersFromString
import nl.tudelft.trustchain.peerchat.ui.contacts.ContactItem
import nl.tudelft.trustchain.valuetransfer.R
import java.text.SimpleDateFormat
import java.util.*

class ChatItemRenderer(
    private val onChatClick: (Contact) -> Unit,
    private val onRequestClick: (Contact) -> Unit,
    private val onTransferClick: (Contact) -> Unit,
) : ItemLayoutRenderer<ContactItem, View>(
    ContactItem::class.java
) {

    override fun bindView(item: ContactItem, view: View) = with(view) {

        val lastMessageDate = item.lastMessage?.timestamp
        val diff = Date().time - lastMessageDate!!.time

        val message = when(item.lastMessage?.message!!.isNotEmpty()) {
            true -> item.lastMessage?.message
            false -> "Attachment"
        }

        val contactTime = when {
            diff <= 24*60*60*1000 -> SimpleDateFormat("HH:mm").format(lastMessageDate)
            diff <= 7*24*60*60*1000 -> SimpleDateFormat("EEEE").format(lastMessageDate)
            else -> SimpleDateFormat("dd/MM/yyyy").format(lastMessageDate)
        }

        val color = getColorByHash(context, item.contact.publicKey.toString())
        val newColor = Color.argb(100, Color.red(color), Color.green(color), Color.blue(color))

        ivChatIconOverview.backgroundTintList = ColorStateList.valueOf(newColor)
        ivChatIconOverview.setBackgroundResource(R.drawable.circle_stroked)

        if(item.lastMessage?.outgoing!!) {
            if(item.lastMessage?.ack!!) {
                ivMessageStatusOverview.setImageResource(R.drawable.ic_check_double)
            }else{
                ivMessageStatusOverview.setImageResource(R.drawable.ic_check_single)
            }
        }else {
            ivMessageStatusOverview.visibility = View.GONE
        }

        tvContactNameOverview.text = item.contact.name
        tvContactMessageOverview.text = message

        if(ContactStore.getInstance(view.context).getContactFromPublicKey(item.contact.publicKey) == null) {
            clContactExchange.visibility = View.GONE
            tvChatInitialsOverview.text = ""
        }else{
            tvChatInitialsOverview.text = getFirstLettersFromString(if(!item.contact.name.isNullOrEmpty()) item.contact.name else "",2)
        }
        ivOnlineStatusOverview.isVisible = item.isOnline || item.isBluetooth
        tvContactTimeOverview.text = contactTime

        clChatCard.setOnClickListener {
            onChatClick(item.contact)
        }

        ivRequest.setOnClickListener {
            onRequestClick(item.contact)
        }

        ivTransfer.setOnClickListener {
            onTransferClick(item.contact)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_contacts_chat
    }
}
