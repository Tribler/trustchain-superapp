package nl.tudelft.trustchain.valuetransfer.ui.exchange

import android.os.Handler
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.util.formatBalance
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*

class ExchangeTransactionItemRenderer(
    private val onSignClick: (TrustChainBlock) -> Unit,
    private val type: String
) : ItemLayoutRenderer<ExchangeTransactionItem, View>(
    ExchangeTransactionItem::class.java
) {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.ENGLISH)

    override fun bindView(item: ExchangeTransactionItem, view: View) = with(view) {

        val currencyAmount = view.findViewById<TextView>(R.id.tvTransactionAmount)
        val transactionDirectionUp = view.findViewById<ImageView>(R.id.ivDirectionIconUp)
        val transactionDirectionDown = view.findViewById<ImageView>(R.id.ivDirectionIconDown)
        val transactionDate = view.findViewById<TextView>(R.id.tvTransactionDate)
        val transactionContent = view.findViewById<ConstraintLayout>(R.id.clTransactionContent)
        val transactionContentText = view.findViewById<TextView>(R.id.tvTransactionContentText)
        val transactionSenderReceiverTitle = view.findViewById<TextView>(R.id.tvTransactionSenderReceiverTitle)
        val transactionSenderReceiverText = view.findViewById<TextView>(R.id.tvTransactionSenderReceiverText)
        val transactionType = view.findViewById<TextView>(R.id.tvTransactionType)
        val transactionTypeText = view.findViewById<TextView>(R.id.tvTransactionTypeText)
        val transactionBlockHash = view.findViewById<TextView>(R.id.tvTransactionBlockHashText)
        val blockStatus = view.findViewById<TextView>(R.id.tvTransactionBlockStatus)
        val blockStatusColorSigned = view.findViewById<ImageView>(R.id.ivTransactionBlockStatusColorSigned)
        val blockStatusColorSelfSigned = view.findViewById<ImageView>(R.id.ivTransactionBlockStatusColorSelfSigned)
        val blockStatusColorWaitingForSignature = view.findViewById<ImageView>(R.id.ivTransactionBlockStatusColorWaitingForSignature)
        val transactionSignButton = view.findViewById<ConstraintLayout>(R.id.clTransactionSignButton)
        val transactionSignButtonView = view.findViewById<TextView>(R.id.tvTransactionSignButton)

        val outgoing = if (type == TYPE_FULL_VIEW) {
            !item.transaction.outgoing
        } else item.transaction.outgoing

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

                transactionType.text = if (outgoing) {
                    if (type == TYPE_FULL_VIEW) {
                        this.context.resources.getString(
                            R.string.text_exchange_transaction_outgoing_to,
                            contact?.name
                                ?: this.context.resources.getString(R.string.text_unknown_contact)
                        )
                    } else {
                        this.context.resources.getString(R.string.text_exchange_transaction_outgoing)
                    }
                } else {
                    if (type == TYPE_FULL_VIEW) {
                        this.context.resources.getString(
                            R.string.text_exchange_transaction_incoming_to,
                            contact?.name ?: this.context.resources.getString(R.string.text_unknown_contact)
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
            currencyAmount.text = "-"
        }

        blockStatus.text = ""

        when (item.status) {
            ExchangeTransactionItem.BlockStatus.SELF_SIGNED -> {
                blockStatus.text = this.context.resources.getString(R.string.text_exchange_self_signed)
                blockStatusColorSelfSigned.isVisible = true
            }
            ExchangeTransactionItem.BlockStatus.SIGNED -> {
                blockStatus.text = this.context.resources.getString(R.string.text_exchange_signed)
                blockStatusColorSigned.isVisible = true
            }
            ExchangeTransactionItem.BlockStatus.WAITING_FOR_SIGNATURE -> {
                blockStatus.text = this.context.resources.getString(R.string.text_exchange_waiting_for_signature)
                blockStatusColorWaitingForSignature.isVisible = true
            }
            null -> {
                blockStatus.text = this.context.resources.getString(R.string.text_exchange_unknown)
            }
        }

        transactionSignButton.isVisible = item.canSign

        transactionSignButton.setOnClickListener {
            transactionSignButton.background = ContextCompat.getDrawable(this.context, R.drawable.pill_rounded_bottom_orange)
            transactionSignButtonView.text = this.context.resources.getString(R.string.text_exchange_signing_transaction)
            Handler().postDelayed(
                Runnable {
                    onSignClick(item.transaction.block)
                },
                2000
            )
        }

        transactionContentText.text = item.transaction.block.transaction.toString()

        transactionSenderReceiverTitle.text = when (item.transaction.type) {
            TransactionRepository.BLOCK_TYPE_CREATE -> this.context.resources.getString(R.string.text_gateway)
            TransactionRepository.BLOCK_TYPE_DESTROY -> this.context.resources.getString(R.string.text_gateway)
            else -> when {
                outgoing -> this.context.resources.getString(R.string.text_transaction_receiver)
                else -> this.context.resources.getString(R.string.text_transaction_sender)
            }
        }

        transactionSenderReceiverText.text = when (item.transaction.type) {
            TransactionRepository.BLOCK_TYPE_CREATE -> item.transaction.sender.keyToBin().toHex()
            TransactionRepository.BLOCK_TYPE_DESTROY -> item.transaction.receiver.keyToBin().toHex()
            else -> item.transaction.sender.keyToBin().toHex()
        }
        transactionTypeText.text = item.transaction.type
        transactionBlockHash.text = item.transaction.block.calculateHash().toHex()

        view.setOnClickListener {
            transactionContent.isVisible = !transactionContent.isVisible
        }
    }

    companion object {
        const val TYPE_FULL_VIEW = "full_view"
        const val TYPE_CONTACT_VIEW = "contact_view"
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_exchange_transaction
    }
}
