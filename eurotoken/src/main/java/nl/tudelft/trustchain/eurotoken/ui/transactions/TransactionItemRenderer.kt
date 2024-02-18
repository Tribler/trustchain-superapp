package nl.tudelft.trustchain.eurotoken.ui.transactions

import android.view.View
import androidx.core.content.ContextCompat
import com.mattskala.itemadapter.ItemLayoutRenderer
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.Gateway
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.Transaction
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.eurotoken.getBalanceForBlock
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat

class TransactionItemRenderer(
    private val transactionRepository: TransactionRepository,
    private val onItemLongClick: (Transaction) -> Unit
) : ItemLayoutRenderer<TransactionItem, View>(
    TransactionItem::class.java
) {
    private val dateFormat = SimpleDateFormat.getDateTimeInstance()

    override fun bindView(item: TransactionItem, view: View) = with(view) {
        val binding = ItemTransactionBinding.bind(view)
        if (item.transaction.type == TransactionRepository.BLOCK_TYPE_CHECKPOINT) {
            binding.txtAmount.text = TransactionRepository.prettyAmount(
                getBalanceForBlock(
                    item.transaction.block,
                    transactionRepository.trustChainCommunity.database
                )!!
            )
            binding.imageInOut.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
            binding.imageInOut.setColorFilter(ContextCompat.getColor(getContext(), R.color.blue))
        } else if (item.transaction.type == TransactionRepository.BLOCK_TYPE_ROLLBACK) {
            binding.txtAmount.text = TransactionRepository.prettyAmount(item.transaction.amount)
            binding.imageInOut.setImageResource(R.drawable.ic_baseline_undo_24)
            binding.imageInOut.setColorFilter(ContextCompat.getColor(getContext(), R.color.red))
        } else {
            binding.txtAmount.text = TransactionRepository.prettyAmount(item.transaction.amount)
            if (item.transaction.outgoing) {
                binding.imageInOut.setImageResource(R.drawable.ic_baseline_outgoing_24)
                binding.imageInOut.setColorFilter(ContextCompat.getColor(getContext(), R.color.red))
            } else {
                binding.imageInOut.setImageResource(R.drawable.ic_baseline_incoming_24)
                binding.imageInOut.setColorFilter(ContextCompat.getColor(getContext(), R.color.green))
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
            val gateway: Gateway? = GatewayStore.getInstance(view.context).getGatewayFromPublicKey(peer)
            binding.txtName.text = gateway?.name ?: ""
        } else {
            val contact: Contact? = ContactStore.getInstance(view.context).getContactFromPublicKey(peer)
            binding.txtName.text = contact?.name ?: ""
        }

        binding.txtDate.text = dateFormat.format(item.transaction.timestamp)
        binding.txtSeq.text = """(${item.transaction.block.sequenceNumber})"""

        binding.txtPeerId.text = peer.keyToHash().toHex()

        binding.txtType.text = item.transaction.type
        if (item.transaction.block.type == TransactionRepository.BLOCK_TYPE_ROLLBACK) {
            binding.txtType.text = "Rollback of seq: "
            binding.txtProp.text =
                "(${transactionRepository.trustChainCommunity.database.getBlockWithHash((item.transaction.block.transaction[TransactionRepository.KEY_TRANSACTION_HASH] as String).hexToBytes())!!.sequenceNumber})"
        } else if (item.transaction.block.isProposal) {
            val linkedBlock = transactionRepository.trustChainCommunity.database.getLinked(
                item.transaction.block)
            if (linkedBlock != null) {
                binding.txtProp.text = "P+A"
                binding.txtProp.setTextColor(ContextCompat.getColor(getContext(), R.color.green));
            } else {
                binding.txtProp.text = "P-A"
                binding.txtProp.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
            }
        } else {
            binding.txtProp.text = "A"
            binding.txtProp.setTextColor(ContextCompat.getColor(getContext(), R.color.green));
        }

        setOnLongClickListener {
            onItemLongClick(item.transaction)
            true
        }

        binding.txtBalance.text = "Balance: " + TransactionRepository.prettyAmount(
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
