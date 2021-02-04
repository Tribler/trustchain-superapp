package nl.tudelft.trustchain.eurotoken.ui.transactions

import android.view.View
import androidx.core.content.ContextCompat
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_transaction.view.*
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.Transaction
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.eurotoken.R
import java.text.SimpleDateFormat

class TransactionItemRenderer(
    private val transactionRepository: TransactionRepository,
    private val onItemLongClick: (Transaction) -> Unit
) : ItemLayoutRenderer<TransactionItem, View>(
    TransactionItem::class.java) {
    private val dateFormat = SimpleDateFormat.getDateTimeInstance()

    override fun bindView(item: TransactionItem, view: View) = with(view) {
        txtAmount.text = TransactionRepository.prettyAmount(item.transaction.amount)

        val contact: Contact?
        val peer: PublicKey
        if (item.transaction.outgoing) {
            imageInOut.setImageResource(R.drawable.ic_baseline_outgoing_24)
            imageInOut.setColorFilter(ContextCompat.getColor(getContext(), R.color.red));
        } else{
            imageInOut.setImageResource(R.drawable.ic_baseline_incoming_24)
            imageInOut.setColorFilter(ContextCompat.getColor(getContext(), R.color.green));
        }
        peer = item.transaction.receiver
        contact = ContactStore.getInstance(view.context).getContactFromPublicKey(peer)

        txtName.text = contact?.name ?: ""
        txtDate.text = dateFormat.format(item.transaction.timestamp)
        txtSeq.text = """(${item.transaction.block.sequenceNumber})"""

        txtPeerId.text = peer.keyToHash().toHex()

        txtType.text = item.transaction.type
        if (item.transaction.block.isProposal) {
            if (transactionRepository.trustChainCommunity.database.getLinked(item.transaction.block) != null) {
                txtProp.text = "P+A"
                txtProp.setTextColor(ContextCompat.getColor(getContext(), R.color.green));
            } else {
                txtProp.text = "P-A"
                txtProp.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
            }
        } else {
            txtProp.text = "A"
            txtProp.setTextColor(ContextCompat.getColor(getContext(), R.color.green));
        }

        setOnLongClickListener {
            onItemLongClick(item.transaction)
            true
        }

        txtBalance.text = TransactionRepository.prettyAmount(transactionRepository.getBalanceForBlock(item.transaction.block))
        //txtVBalance.text = TransactionRepository.prettyAmount(transactionRepository.getVerifiedBalanceForBlock(item.transaction.block))
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_transaction
    }

}
