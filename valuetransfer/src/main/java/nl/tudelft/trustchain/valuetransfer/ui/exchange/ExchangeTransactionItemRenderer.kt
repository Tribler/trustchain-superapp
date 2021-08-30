package nl.tudelft.trustchain.valuetransfer.ui.exchange

import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_contacts_chat_detail.view.*
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.Transaction
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.util.formatBalance
import java.math.BigInteger
import java.text.SimpleDateFormat

class ExchangeTransactionItemRenderer(
    private val onSignClick: (TrustChainBlock) -> Unit,
) : ItemLayoutRenderer<ExchangeTransactionItem, View>(
    ExchangeTransactionItem::class.java
) {

    override fun bindView(item: ExchangeTransactionItem, view: View) = with(view) {

//        val currencyName = view.findViewById<TextView>(R.id.tvTransactionCurrencyTitle)
        val currencyAmount = view.findViewById<TextView>(R.id.tvTransactionAmount)
//        val currencySymbol = view.findViewById<TextView>(R.id.tvTransactionCurrencySymbol)
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
//        val canSignButton = view.findViewById<TextView>(R.id.tvSignButton)

        val outgoing = !item.transaction.outgoing

        when (item.transaction.type) {
            TransactionRepository.BLOCK_TYPE_CREATE -> {
                transactionType.text = "Buy from EuroToken Exchange"
                transactionDirectionUp.isVisible = outgoing
                transactionDirectionDown.isVisible = !outgoing
            }
            TransactionRepository.BLOCK_TYPE_DESTROY -> {
                transactionType.text = "Sell to EuroToken Exchange"
                transactionDirectionUp.isVisible = outgoing
                transactionDirectionDown.isVisible = !outgoing
            }
            TransactionRepository.BLOCK_TYPE_TRANSFER -> {

    //            val outgoing = item.transaction.sender == transactionRepository.trustChainCommunity.myPeer.publicKey

                val contact = ContactStore.getInstance(view.context).getContactFromPublicKey(item.transaction.sender)
    //            if(item.transaction.outgoing) {
                transactionType.text = if(outgoing) {
                    "Outgoing transfer to ${contact?.name ?: "unknown contact"}"
                }else{
                    "Incoming transfer from ${contact?.name ?: "unknown contact"}"
                }

                transactionDirectionUp.isVisible = !outgoing
                transactionDirectionDown.isVisible = outgoing
            }
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm")
        transactionDate.text = dateFormat.format(item.transaction.timestamp)

        val map = item.transaction.block.transaction.toMap()
        if(map.containsKey("amount")) {
            currencyAmount.text = formatBalance((map["amount"] as BigInteger).toLong())
        }else{
            currencyAmount.text = "-"
        }

        blockStatus.text = ""

        when(item.status) {
            ExchangeTransactionItem.BlockStatus.SELF_SIGNED -> {
                blockStatus.text = "Self-Signed"
                blockStatusColorSelfSigned.isVisible = true
            }
            ExchangeTransactionItem.BlockStatus.SIGNED -> {
                blockStatus.text = "Signed"
                blockStatusColorSigned.isVisible = true
            }
            ExchangeTransactionItem.BlockStatus.WAITING_FOR_SIGNATURE -> {
                blockStatus.text = "Waiting for signature"
                blockStatusColorWaitingForSignature.isVisible = true
            }
            null -> {
                blockStatus.text = "Status unknown"
            }
        }

        transactionSignButton.isVisible = item.canSign

        transactionSignButton.setOnClickListener {
            transactionSignButton.background = ContextCompat.getDrawable(this.context, R.drawable.pill_rounded_bottom_orange)
            transactionSignButtonView.text = "Signing transaction..."
            Handler().postDelayed(
                Runnable {
                    onSignClick(item.transaction.block)
                }, 2000
            )
        }

        transactionContentText.text = item.transaction.block.transaction.toString()

        transactionSenderReceiverTitle.text = when {
            outgoing -> "Receiver"
                else -> "Sender"
        }

        transactionSenderReceiverText.text = item.transaction.sender.keyToBin().toHex()
        transactionTypeText.text = item.transaction.type
        transactionBlockHash.text = item.transaction.block.calculateHash().toHex()

        view.setOnClickListener {
            transactionContent.isVisible = !transactionContent.isVisible
//            transactionContent.visibility = if(transactionContent.visibility == View.GONE) View.VISIBLE else View.GONE
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_exchange_transaction
    }
}
