package nl.tudelft.trustchain.eurotoken.ui.transactions

import android.view.View
import androidx.core.content.ContextCompat
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_transaction.view.*
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.*
import nl.tudelft.trustchain.eurotoken.R
import java.text.SimpleDateFormat

class TransactionItemRenderer(
    private val transactionRepository: TransactionRepository,
    private val onItemLongClick: (Transaction) -> Unit
) : ItemLayoutRenderer<TransactionItem, View>(
    TransactionItem::class.java
) {
    private val dateFormat = SimpleDateFormat.getDateTimeInstance()

    override fun bindView(item: TransactionItem, view: View) = with(view) {
        if (item.transaction.type == TransactionRepository.BLOCK_TYPE_CHECKPOINT) {
            txtAmount.text = TransactionRepository.prettyAmount(
                getBalanceForBlock(
                    item.transaction.block,
                    transactionRepository.trustChainCommunity.database
                )!!
            )
            imageInOut.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
            imageInOut.setColorFilter(ContextCompat.getColor(getContext(), R.color.blue))
        } else if (item.transaction.type == TransactionRepository.BLOCK_TYPE_ROLLBACK) {
            txtAmount.text = TransactionRepository.prettyAmount(item.transaction.amount)
            imageInOut.setImageResource(R.drawable.ic_baseline_undo_24)
            imageInOut.setColorFilter(ContextCompat.getColor(getContext(), R.color.red))
        } else {
            txtAmount.text = TransactionRepository.prettyAmount(item.transaction.amount)
            if (item.transaction.outgoing) {
                imageInOut.setImageResource(R.drawable.ic_baseline_outgoing_24)
                imageInOut.setColorFilter(ContextCompat.getColor(getContext(), R.color.red))
            } else {
                imageInOut.setImageResource(R.drawable.ic_baseline_incoming_24)
                imageInOut.setColorFilter(ContextCompat.getColor(getContext(), R.color.green))
            }
        }
        val peer: PublicKey
        peer = item.transaction.receiver
        if (listOf(
                TransactionRepository.BLOCK_TYPE_DESTROY,
                TransactionRepository.BLOCK_TYPE_CREATE,
                TransactionRepository.BLOCK_TYPE_CHECKPOINT
            ).contains(item.transaction.type)
        ) {
            val gateway: Gateway?
            gateway = GatewayStore.getInstance(view.context).getGatewayFromPublicKey(peer)
            txtName.text = gateway?.name ?: ""
        } else {
            val contact: Contact?
            contact = ContactStore.getInstance(view.context).getContactFromPublicKey(peer)
            txtName.text = contact?.name ?: ""
        }

        txtDate.text = dateFormat.format(item.transaction.timestamp)
        txtSeq.text = """(${item.transaction.block.sequenceNumber})"""

        txtPeerId.text = peer.keyToHash().toHex()

        txtType.text = item.transaction.type
        if (item.transaction.block.type == TransactionRepository.BLOCK_TYPE_ROLLBACK) {
            txtType.text = "Rollback of seq: "
            txtProp.text =
                "(${transactionRepository.trustChainCommunity.database.getBlockWithHash((item.transaction.block.transaction[TransactionRepository.KEY_TRANSACTION_HASH] as String).hexToBytes())!!.sequenceNumber})"
        } else if (item.transaction.block.isProposal) {
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

        txtBalance.text = "Balance: " + TransactionRepository.prettyAmount(
            getBalanceForBlock(
                item.transaction.block,
                transactionRepository.trustChainCommunity.database
            )!!
        )
//        txtVBalance.text = TransactionRepository.prettyAmount(transactionRepository.getVerifiedBalanceForBlock(item.transaction.block, transactionRepository.trustChainCommunity.database)!!)
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_transaction
    }

}
