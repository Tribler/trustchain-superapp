package nl.tudelft.trustchain.offlinedigitaleuro.ui.transactions

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.liveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.*
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinedigitaleuro.R
import nl.tudelft.trustchain.offlinedigitaleuro.databinding.TransactionsFragmentBinding
import nl.tudelft.trustchain.offlinedigitaleuro.db.Transactions
import nl.tudelft.trustchain.offlinedigitaleuro.ui.OfflineDigitalEuroBaseFragment

class TransactionsFragment : OfflineDigitalEuroBaseFragment(R.layout.transactions_fragment) {
    private val binding by viewBinding(TransactionsFragmentBinding::bind)

    private val adapter = ItemAdapter()

    private val items: LiveData<List<Item>> by lazy {
        liveData { emit(listOf()) }
    }

    @SuppressLint("SetTextI18n")
    private fun updateBalance() {
        val euro1 = db.tokensDao().getCountTokensOfValue(1.0)
        val euro2 = db.tokensDao().getCountTokensOfValue(2.0)
        val euro5 = db.tokensDao().getCountTokensOfValue(5.0)
        val euro10 = db.tokensDao().getCountTokensOfValue(10.0)

        var sum = 0

        sum += euro1 * 1 + euro2 * 2 + euro5 * 5 + euro10 * 10

        binding.txtBalance.text = sum.toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(TransactionItemRenderer())

        lifecycleScope.launchWhenResumed {
            while (isActive) {
                updateBalance()
                val items = db.transactionsDao().getTransactionData()
                    .map { transaction: Transactions -> TransactionItem(transaction) }
                adapter.updateItems(items)
                adapter.notifyDataSetChanged()
                delay(1000L)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.trustScoresRecyclerView.adapter = adapter
        binding.trustScoresRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.trustScoresRecyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayout.VERTICAL
            )
        )

        items.observe(viewLifecycleOwner) {
            adapter.updateItems(it)
        }
    }
}
