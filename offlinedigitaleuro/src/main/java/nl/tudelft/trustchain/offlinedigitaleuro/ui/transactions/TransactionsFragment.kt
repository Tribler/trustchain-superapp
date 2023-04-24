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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinedigitaleuro.R
import nl.tudelft.trustchain.offlinedigitaleuro.databinding.TransactionsFragmentBinding
import nl.tudelft.trustchain.offlinedigitaleuro.db.Transactions
import nl.tudelft.trustchain.offlinedigitaleuro.ui.OfflineDigitalEuroBaseFragment

class TransactionsFragment : OfflineDigitalEuroBaseFragment(R.layout.transactions_fragment) {
    private val binding by viewBinding(TransactionsFragmentBinding::bind)

    @SuppressLint("SetTextI18n")
    private fun updateBalance() {
        binding.txtBalance.text = (db.tokensDao().getCountTokensOfValue(1.0) +
            db.tokensDao().getCountTokensOfValue(2.0) +
            db.tokensDao().getCountTokensOfValue(5.0) +
            db.tokensDao().getCountTokensOfValue(10.0)).toString()
    }

    private val adapter = ItemAdapter()

    private val items: LiveData<List<Item>> by lazy {
        liveData { emit(listOf()) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runBlocking(Dispatchers.IO) {
            updateBalance()
        }

        adapter.registerRenderer(TransactionItemRenderer())

        lifecycleScope.launch(Dispatchers.IO) {
            val items = db.transactionsDao().getTransactionData().map { transaction: Transactions -> TransactionItem(transaction) }
            adapter.updateItems(items)
            adapter.notifyDataSetChanged()
            delay(1000L)
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
