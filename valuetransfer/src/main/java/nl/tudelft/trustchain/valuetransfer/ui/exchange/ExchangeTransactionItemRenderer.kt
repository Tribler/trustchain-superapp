package nl.tudelft.trustchain.valuetransfer.ui.exchange

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContentProviderCompat.requireContext
import com.mattskala.itemadapter.ItemLayoutRenderer
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.Transaction
import nl.tudelft.trustchain.valuetransfer.R
import java.math.BigInteger
import java.text.SimpleDateFormat

class ExchangeTransactionItemRenderer(
    private val onTransactionClick: (Transaction) -> Unit
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

        transactionDirectionUp.visibility = if(item.transaction.outgoing) View.GONE else View.VISIBLE
        transactionDirectionDown.visibility = if(!item.transaction.outgoing) View.GONE else View.VISIBLE

        if(item.transaction.type == "eurotoken_creation") {
            transactionType.text = "Buy from EuroToken Exchange"
        }else if(item.transaction.type == "eurotoken_destruction") {
            transactionType.text = "Sell to EuroToken Exchange"
        }else if(item.transaction.type == "eurotoken_transfer") {

            val contact = ContactStore.getInstance(view.context).getContactFromPublicKey(item.transaction.receiver)
            if(item.transaction.outgoing) {
                transactionType.text = "Outgoing transfer to ${contact?.name ?: "unknown contact"}"
            }else{
                transactionType.text = "Incoming transfer from ${contact?.name ?: "unknown contact"}"
            }
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm")
        transactionDate.text = dateFormat.format(item.transaction.timestamp)

        val map = item.transaction.block.transaction.toMap()
        if(map.containsKey("amount")) {
            currencyAmount.text = (map["amount"] as BigInteger).toDouble().div(100).toString()
        }else{
            currencyAmount.text = "-"
        }

        when(item.status) {
            ExchangeTransactionItem.BlockStatus.SELF_SIGNED -> {
                blockStatus.text = "Self-Signed"
                blockStatusColorSelfSigned.visibility = View.VISIBLE
            }
            ExchangeTransactionItem.BlockStatus.SIGNED -> {
                blockStatus.text = "Signed"
                blockStatusColorSigned.visibility = View.VISIBLE
            }
            ExchangeTransactionItem.BlockStatus.WAITING_FOR_SIGNATURE -> {
                blockStatus.text = "Waiting for signature"
                blockStatusColorWaitingForSignature.visibility = View.VISIBLE
            }
            null -> {
                blockStatus.text = "Status unknown"
            }
        }

        transactionContentText.text = item.transaction.block.transaction.toString()

        when(item.transaction.outgoing) {
            true -> {
                transactionSenderReceiverTitle.text = "Receiver"
                transactionSenderReceiverText.text = item.transaction.receiver.keyToBin().toHex()
            }
            false -> {
                transactionSenderReceiverTitle.text = "Sender"
                transactionSenderReceiverText.text = item.transaction.receiver.keyToBin().toHex()
            }
        }
        transactionTypeText.text = item.transaction.type
        transactionBlockHash.text = item.transaction.block.calculateHash().toHex()

        view.setOnClickListener {
            transactionContent.visibility = if(transactionContent.visibility == View.GONE) View.VISIBLE else View.GONE
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_exchange_transaction
    }
}
