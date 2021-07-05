package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_chat.view.*
import kotlinx.android.synthetic.main.item_chat.view.tvContactName
import kotlinx.android.synthetic.main.item_chat_overview.view.*
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.common.util.getFirstLettersFromString
import nl.tudelft.trustchain.peerchat.ui.contacts.ContactItem
import nl.tudelft.trustchain.valuetransfer.R
import java.text.SimpleDateFormat
import java.util.*

class ChatItemRenderer(
    private val onChatClick: (Contact) -> Unit,
    private val onExchangeClick: (Contact) -> Unit,
    private val layoutType: Int,
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

        if(layoutType == 0) {
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
            tvChatInitialsOverview.text = getFirstLettersFromString(if(!item.contact.name.isNullOrEmpty()) item.contact.name else "",2)
            ivOnlineStatusOverview.isVisible = item.isOnline || item.isBluetooth
            tvContactTimeOverview.text = contactTime
        }else {
            ivChatIcon.backgroundTintList = ColorStateList.valueOf(newColor)
            ivChatIcon.setBackgroundResource(R.drawable.circle_stroked)

            if(item.lastMessage?.outgoing!!) {
                if(item.lastMessage?.ack!!) {
                    ivMessageState.setImageResource(R.drawable.ic_check_double)
                }else{
                    ivMessageState.setImageResource(R.drawable.ic_check_single)
                }
            }else {
                ivMessageState.visibility = View.GONE
            }

            tvContactName.text = item.contact.name
            tvContactMessage.text = message
            tvChatInitials.text = getFirstLettersFromString(if(!item.contact.name.isNullOrEmpty()) item.contact.name else "",2)
            ivOnlineStatus.isVisible = item.isOnline || item.isBluetooth
            tvContactTime.text = contactTime
        }

//        setOnClickListener {
//            onChatClick(item.contact)
//        }

        clChatCard.setOnClickListener {
            onChatClick(item.contact)
        }

        clContactExchange.setOnClickListener {
            onExchangeClick(item.contact)
        }
    }

    override fun getLayoutResourceId(): Int {
        return when(layoutType) {
            0 -> R.layout.item_chat_overview
            1 -> R.layout.item_chat
            else -> R.layout.item_chat
        }
    }
}
