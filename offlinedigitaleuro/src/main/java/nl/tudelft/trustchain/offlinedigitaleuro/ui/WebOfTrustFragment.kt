package nl.tudelft.trustchain.offlinedigitaleuro.ui

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
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinedigitaleuro.R
import nl.tudelft.trustchain.offlinedigitaleuro.databinding.WebOfTrustFragmentBinding
import nl.tudelft.trustchain.offlinedigitaleuro.db.WebOfTrust

class WebOfTrustFragment : OfflineDigitalEuroBaseFragment(R.layout.web_of_trust_fragment) {
    private val binding by viewBinding(WebOfTrustFragmentBinding::bind)

    private val adapter = ItemAdapter()

    private val items: LiveData<List<Item>> by lazy {
        liveData { emit(listOf()) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(TrustScoreItemRenderer())

        lifecycleScope.launch(Dispatchers.IO) {
            val items = db.webOfTrustDao().getAllTrustScores().map { trustScore: WebOfTrust -> TrustScoreItem(trustScore) }
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
