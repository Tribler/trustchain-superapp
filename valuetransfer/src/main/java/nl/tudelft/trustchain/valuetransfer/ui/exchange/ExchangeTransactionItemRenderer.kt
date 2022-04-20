package nl.tudelft.trustchain.valuetransfer.ui.exchange

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.util.formatBalance
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*

class ExchangeTransactionItemRenderer(
    private val withName: Boolean,
    private val parentActivity: ValueTransferMainActivity,
    private val onTransactionClick: (ExchangeTransactionItem) -> Unit
) : ItemLayoutRenderer<ExchangeTransactionItem, View>(
    ExchangeTransactionItem::class.java
) {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.ENGLISH)

    override fun bindView(item: ExchangeTransactionItem, view: View) = with(view) {

        val currencyAmount = view.findViewById<TextView>(R.id.tvTransactionAmount)
        val transactionDirectionUp = view.findViewById<ImageView>(R.id.ivDirectionIconUp)
        val transactionDirectionDown = view.findViewById<ImageView>(R.id.ivDirectionIconDown)
        val transactionDate = view.findViewById<TextView>(R.id.tvTransactionDate)
        val transactionType = view.findViewById<TextView>(R.id.tvTransactionType)
        val blockStatus = view.findViewById<TextView>(R.id.tvTransactionBlockStatus)
        val blockStatusColorSigned = view.findViewById<ImageView>(R.id.ivTransactionBlockStatusColorSigned)
        val blockStatusColorSelfSigned = view.findViewById<ImageView>(R.id.ivTransactionBlockStatusColorSelfSigned)
        val blockStatusColorWaitingForSignature = view.findViewById<ImageView>(R.id.ivTransactionBlockStatusColorWaitingForSignature)

        val outgoing = !item.transaction.outgoing

        when (item.transaction.type) {
            TransactionRepository.BLOCK_TYPE_CREATE -> {
                transactionType.text = this.context.resources.getString(R.string.text_exchange_buy)
                transactionDirectionUp.isVisible = outgoing
                transactionDirectionDown.isVisible = !outgoing
            }
            TransactionRepository.BLOCK_TYPE_DESTROY -> {
                transactionType.text = this.context.resources.getString(R.string.text_exchange_sell)
                transactionDirectionUp.isVisible = outgoing
                transactionDirectionDown.isVisible = !outgoing
            }
            TransactionRepository.BLOCK_TYPE_TRANSFER -> {
                val contact = ContactStore.getInstance(view.context).getContactFromPublicKey(item.transaction.sender)
                val contactName = contact?.name
                val unknownName = resources.getString(R.string.text_unknown_contact)

                transactionType.text = if (outgoing) {
                    if (withName) {
                        this.context.resources.getString(
                            R.string.text_exchange_transaction_outgoing_to,
                            getName(item.transaction.sender) ?: (contactName ?: unknownName)
                        )
                    } else {
                        this.context.resources.getString(R.string.text_exchange_transaction_outgoing)
                    }
                } else {
                    if (withName) {
                        this.context.resources.getString(
                            R.string.text_exchange_transaction_incoming_to,
                            getName(item.transaction.sender) ?: (contactName ?: unknownName)
                        )
                    } else {
                        this.context.resources.getString(R.string.text_exchange_transaction_incoming)
                    }
                }

                transactionDirectionUp.isVisible = !outgoing
                transactionDirectionDown.isVisible = outgoing
            }
        }

        transactionDate.text = dateFormat.format(item.transaction.timestamp)

        val map = item.transaction.block.transaction.toMap()
        if (map.containsKey("amount")) {
            currencyAmount.text = formatBalance((map["amount"] as BigInteger).toLong())
        } else {
            currencyAmount.text = "??"
        }

        blockStatus.text = when (item.status) {
            ExchangeTransactionItem.BlockStatus.SELF_SIGNED -> {
                blockStatusColorSelfSigned.isVisible = true
                this.context.resources.getString(R.string.text_exchange_self_signed)
            }
            ExchangeTransactionItem.BlockStatus.SIGNED -> {
                blockStatusColorSigned.isVisible = true
                this.context.resources.getString(R.string.text_exchange_signed)
            }
            ExchangeTransactionItem.BlockStatus.WAITING_FOR_SIGNATURE -> {
                blockStatusColorWaitingForSignature.isVisible = true
                this.context.resources.getString(R.string.text_exchange_waiting_for_signature)
            }
            null -> {
                this.context.resources.getString(R.string.text_exchange_unknown)
            }
        }

        view.setOnClickListener {
            onTransactionClick(item)
        }
    }

    fun getName(publicKey: PublicKey): String? {
        val contactState = parentActivity.getStore<PeerChatStore>()!!
            .getContactState(publicKey)?.identityInfo
        val identityInitials = contactState?.initials
        val identitySurname = contactState?.surname

        return if (identityInitials != null && identitySurname != null) {
            StringBuilder().append(identityInitials).append(" ").append(identitySurname)
                .toString()
        } else null
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_exchange_transaction
    }
}
