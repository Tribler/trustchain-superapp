package nl.tudelft.trustchain.eurotoken.ui.transactions

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.eurotoken.dummy.DummyContent
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.ContactStore
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

    private val contactsStore by lazy {
        ContactStore.getInstance(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(TransactionItemRenderer())

        lifecycleScope.launchWhenResumed {
            while (isActive) {
                // Refresh peer status periodically
                val items = transactionRepository.getTransactions().map { block -> TransactionItem(block) }
                adapter.updateItems(items)
                delay(1000L)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.list.adapter = adapter
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))

        items.observe(viewLifecycleOwner, Observer {
            adapter.updateItems(it)
        })
    }
}
