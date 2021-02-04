package nl.tudelft.trustchain.eurotoken.ui.transactions

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.liveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.Transaction
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.databinding.FragmentTransactionsBinding
import nl.tudelft.trustchain.eurotoken.ui.EurotokenBaseFragment

/**
 * A fragment representing a list of Items.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsFragment : EurotokenBaseFragment(R.layout.fragment_transactions) {
    private val binding by viewBinding(FragmentTransactionsBinding::bind)

    private val adapter = ItemAdapter()

    private val items: LiveData<List<Item>> by lazy {
        liveData { emit(listOf<Item>()) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fun resendBlock(transaction: Transaction) {
            transactionRepository.trustChainCommunity.sendBlock(transaction.block)
        }

        fun showOptions(transaction: Transaction) {
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
                val ownContact = ContactStore.getInstance(requireContext()).getContactFromPublicKey(ownKey)

                // Refresh peer status periodically
                val items = transactionRepository.getTransactions().map { block : Transaction -> TransactionItem(
                    block
                ) }
                adapter.updateItems(items)
                binding.txtBalance.text = TransactionRepository.prettyAmount(transactionRepository.getMyBalance())
                if (ownContact?.name != null) {
                    binding.txtOwnName.text = "Your balance (" + ownContact?.name + ")"
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
                TransactionRepository.prettyAmount(transactionRepository.getMyBalance())
        })
    }
}
