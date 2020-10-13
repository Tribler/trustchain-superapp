package nl.tudelft.trustchain.eurotoken.ui.transactions

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.*
import androidx.recyclerview.widget.DividerItemDecoration
import nl.tudelft.trustchain.common.ui.BaseFragment
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.databinding.FragmentTransactionsBinding

/**
 * A fragment representing a list of Items.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsFragment : BaseFragment(R.layout.fragment_transactions) {
    private val binding by viewBinding(FragmentTransactionsBinding::bind)

    private var columnCount = 1

    private val adapter = ItemAdapter()

    private val items: LiveData<List<Item>> by lazy {
        liveData { emit(listOf<Item>()) }
    }

    private val transactionRepository by lazy {
        TransactionRepository(getIpv8().getOverlay()!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(TransactionItemRenderer())

        lifecycleScope.launchWhenResumed {
            while (isActive) {
                // Refresh peer status periodically
                val items = transactionRepository.getTransactions().map { block -> TransactionItem(block) }
                adapter.updateItems(items)
                binding.txtBalance.text = TransactionRepository.prettyAmount(transactionRepository.getBalance())
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
            binding.txtBalance.text = TransactionRepository.prettyAmount(transactionRepository.getBalance())
        })
    }
}
