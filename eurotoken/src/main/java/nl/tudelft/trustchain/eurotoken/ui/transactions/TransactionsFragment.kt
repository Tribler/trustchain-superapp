package nl.tudelft.trustchain.eurotoken.ui.transactions

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.liveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.Transaction
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.community.EuroTokenCommunity
import nl.tudelft.trustchain.eurotoken.databinding.FragmentTransactionsBinding
import nl.tudelft.trustchain.eurotoken.ui.EurotokenBaseFragment

class TransactionsFragment : EurotokenBaseFragment(R.layout.fragment_transactions) {
    private val binding by viewBinding(FragmentTransactionsBinding::bind)

    private val adapter = ItemAdapter()

    private val items: LiveData<List<Item>> by lazy {
        liveData { emit(listOf<Item>()) }
    }

    @JvmName("getEuroTokenCommunity1")
    private fun getEuroTokenCommunity(): EuroTokenCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("EuroTokenCommunity is not configured")
    }

    private val euroTokenCommunity by lazy {
        getEuroTokenCommunity()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fun resendBlock(transaction: Transaction) {
            val key = defaultCryptoProvider.keyFromPublicBin(transaction.block.linkPublicKey)
            val peer = Peer(key)
            transactionRepository.trustChainCommunity.sendBlock(transaction.block, peer)
        }

        fun payBack(transaction: Transaction) {
            if (transaction.block.isAgreement) {
                transactionRepository.sendTransferProposal(
                    recipient = transaction.block.linkPublicKey,
                    amount = transaction.amount
                ) ?: return Toast.makeText(
                    requireContext(),
                    "Insufficient balance",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                transactionRepository.sendTransferProposal(
                    recipient = transaction.block.publicKey,
                    amount = transaction.amount
                )
            }
        }

        fun rollBack(transaction: Transaction) {
            if (transaction.block.isAgreement) {
                transactionRepository.attemptRollback(null, transaction.block.calculateHash())
            }
        }

        fun requestRollback(transaction: Transaction) {
            val key = defaultCryptoProvider.keyFromPublicBin(transaction.block.linkPublicKey)
            val peer = Peer(key)
            val linked =
                transactionRepository.trustChainCommunity.database.getLinked(transaction.block)
                    ?: return
            euroTokenCommunity.requestRollback(linked.calculateHash(), peer)
        }

        fun showOptions(transaction: Transaction) {
            if (!transaction.outgoing && transaction.block.type == TransactionRepository.BLOCK_TYPE_TRANSFER) {
                val items = arrayOf("Resend", "Pay back", "Roll back")
                AlertDialog.Builder(requireContext())
                    .setItems(items) { _, which ->
                        when (which) {
                            0 -> resendBlock(transaction)
                            1 -> payBack(transaction)
                            2 -> rollBack(transaction)
                        }
                    }
                    .show()
                return
            }
            val items = arrayOf("Resend")
            AlertDialog.Builder(requireContext())
                .setItems(items) { _, which ->
                    when (which) {
                        0 -> resendBlock(transaction)
                    }
                }
                .show()
        }

        adapter.registerRenderer(TransactionItemRenderer(transactionRepository) { showOptions(it) })

        lifecycleScope.launchWhenResumed {
            while (isActive) {

                val ownKey = transactionRepository.trustChainCommunity.myPeer.publicKey
                val ownContact =
                    ContactStore.getInstance(requireContext()).getContactFromPublicKey(ownKey)

                // Refresh transactions periodically
                val items = transactionRepository.getTransactions().map { block: Transaction ->
                    TransactionItem(
                        block
                    )
                }
                adapter.updateItems(items)
                adapter.notifyDataSetChanged()
                binding.txtBalance.text =
                    TransactionRepository.prettyAmount(transactionRepository.getMyVerifiedBalance())
                if (ownContact?.name != null) {
                    binding.txtOwnName.text = "Your balance (" + ownContact.name + ")"
                }
                delay(1000L)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.list.adapter = adapter
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
        binding.txtOwnPublicKey.text = getTrustChainCommunity().myPeer.publicKey.keyToHash().toHex()

        items.observe(viewLifecycleOwner, Observer {
            adapter.updateItems(it)
            binding.txtBalance.text =
                TransactionRepository.prettyAmount(transactionRepository.getMyVerifiedBalance())
        })
    }
}
