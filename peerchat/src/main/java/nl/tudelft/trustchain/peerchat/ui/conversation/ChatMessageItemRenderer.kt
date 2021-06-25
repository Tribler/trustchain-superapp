package nl.tudelft.trustchain.peerchat.ui.conversation

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.findFragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.fragment_add_remote.*
import kotlinx.android.synthetic.main.item_message.view.*
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.peerchat.R
import nl.tudelft.trustchain.peerchat.ui.addcontact.AddContactFragment
import java.math.BigInteger
import java.text.SimpleDateFormat

class ChatMessageItemRenderer : ItemLayoutRenderer<ChatMessageItem, View>(
    ChatMessageItem::class.java
) {

    private val dateTimeFormat = SimpleDateFormat.getDateTimeInstance()
    private val timeFormat = SimpleDateFormat.getTimeInstance()
    private val constraintSet = ConstraintSet()

    override fun bindView(item: ChatMessageItem, view: View) = with(view) {
        txtMessage.text = item.chatMessage.message
        if (item.chatMessage.outgoing) {
            txtMessage.gravity = Gravity.START
        } else {
            txtMessage.gravity = Gravity.END
        }
        item.transaction?.transaction?.let {
            txtTransaction.text =
                TransactionRepository.prettyAmount((item.transaction.transaction["amount"] as BigInteger).toLong())
            if (item.chatMessage.message.isEmpty()) {
                txtMessage.visibility = View.GONE
            }
        }
        val color = getColorByHash(context, item.chatMessage.sender.toString())
        val newColor = Color.argb(50, Color.red(color), Color.green(color), Color.blue(color))
        txtMessage.backgroundTintList = ColorStateList.valueOf(newColor)
        txtDate.text = if (DateUtils.isToday(item.chatMessage.timestamp.time))
            timeFormat.format(item.chatMessage.timestamp)
        else dateTimeFormat.format(item.chatMessage.timestamp)
        txtDate.isVisible = item.shouldShowDate
        avatar.setUser(item.chatMessage.sender.toString(), item.participantName)
        avatar.isVisible = item.shouldShowAvatar
        imgDelivered.isVisible = item.chatMessage.outgoing && item.chatMessage.ack
        constraintSet.clone(constraintLayout)
        constraintSet.removeFromHorizontalChain(content.id)
        constraintSet.removeFromHorizontalChain(bottomContainer.id)
        contactLayout.isVisible = false

        val attachment = item.chatMessage.attachment
        if (attachment != null && attachment.type == MessageAttachment.TYPE_IMAGE) {
            val file = attachment.getFile(view.context)
            if (file.exists()) {
                Glide.with(view).load(file).into(image)
                progress.isVisible = false
            } else {
                image.setImageBitmap(null)
                progress.isVisible = true
            }
            image.isVisible = true
            txtMessage.isVisible = false
            txtTransaction.isVisible = false
        } else if (attachment != null && attachment.type == MessageAttachment.TYPE_CONTACT) {
            contactLayout.isVisible = true
            val contact = Contact.deserialize(attachment.content, 0).first
            contactName.text =contact.name
            progress.isVisible = false
            txtMessage.isVisible = false
            txtTransaction.isVisible = false
            image.isVisible = false
            if (!item.chatMessage.outgoing) {
                contactLayout.setOnClickListener {
                    val args = Bundle()
                    val publicKeyBin = contact.publicKey.keyToBin().toHex()
                    args.putString(AddContactFragment.ARG_NAME, contact.name)
                    args.putString(AddContactFragment.ARG_PUBLIC_KEY, publicKeyBin)
                    view.findFragment<ConversationFragment>().
                    findNavController().navigate(
                        R.id.action_conversationFragment_to_addContactFragment,
                        args
                    )
                }
            }
        } else if (item.chatMessage.transactionHash != null) {
            progress.isVisible =
                item.transaction == null // transaction not yet received via trustchain

            txtMessage.isVisible = item.chatMessage.message.isNotBlank()

            image.isVisible = false
            txtTransaction.isVisible = true
        } else {
            image.isVisible = false
            txtMessage.isVisible = true
            progress.isVisible = false
            txtTransaction.isVisible = false
        }

        if (item.chatMessage.outgoing) {
            innerContent.updateLayoutParams<FrameLayout.LayoutParams> {
                gravity = Gravity.END
            }
            constraintSet.connect(
                content.id,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START
            )
            constraintSet.connect(
                content.id,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END
            )
            constraintSet.connect(
                bottomContainer.id,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END
            )
        } else {
            val avatarMargin = resources.getDimensionPixelSize(R.dimen.avatar_size) +
                resources.getDimensionPixelSize(R.dimen.avatar_margin)
            innerContent.updateLayoutParams<FrameLayout.LayoutParams> {
                gravity = Gravity.START
            }
            constraintSet.connect(
                content.id,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                avatarMargin
            )
            constraintSet.connect(
                content.id,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END
            )
            constraintSet.connect(
                bottomContainer.id,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                avatarMargin
            )
        }
        constraintSet.applyTo(constraintLayout)
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_message
    }
}
