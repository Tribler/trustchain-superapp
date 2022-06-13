package nl.tudelft.trustchain.valuetransfer.ui.trust

import android.os.Bundle
import android.view.*
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.*
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.util.*
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.ui.VTFragment
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentTrustVtBinding

class TrustFragment : VTFragment(R.layout.fragment_trust_vt) {
    private val binding by viewBinding(FragmentTrustVtBinding::bind)

    private val adapterTransactions = ItemAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trust_vt, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        adapterTransactions.registerRenderer(
            TrustItemRenderer(parentActivity)
        )

        lifecycleScope.launchWhenStarted {
            while (isActive) {
                refreshTransactions()

                delay(1000)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        onResume()

        binding.rvScores.apply {
            adapter = adapterTransactions
            layoutManager = LinearLayoutManager(requireContext())
            val drawable = ResourcesCompat.getDrawable(
                resources,
                R.drawable.divider_transaction,
                requireContext().theme
            )
            addItemDecoration(DividerItemDecorator(drawable!!) as RecyclerView.ItemDecoration)
        }
    }

    override fun initView() {
        parentActivity.apply {
            setActionBarTitle(getString(R.string.text_title_trust_scores), null)
            toggleActionBar(true)
            toggleBottomNavigation(false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.trust_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()

                return true
            }
            R.id.actionClearScores -> {
                clearStore()
            }
            R.id.actionInsertScores -> {
                addScores()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onBackPressed(animated: Boolean = true) {
        val previousFragment = parentFragmentManager.fragments.filter {
            it.tag == ValueTransferMainActivity.walletOverviewFragmentTag
        }

        parentFragmentManager.beginTransaction().apply {
            if (animated) setCustomAnimations(0, R.anim.exit_to_right)
            hide(this@TrustFragment)
            if (animated) setCustomAnimations(R.anim.enter_from_left, 0)
            show(previousFragment[0])
        }.commit()

        (previousFragment[0] as VTFragment).initView()
    }

    /**
     * Clear all scores from the store.
     * Demo/Debug functionality.
     */
    private fun clearStore() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                getTrustStore().clearAllTables()
            }
        }
    }

    /**
     * Add 10 random scores to the store.
     * Demo/Debug functionality.
     */
    private fun addScores() {
        lifecycleScope.launch {
            val scores = getTrustCommunity().generateScores(10)
            getTrustStore().trustDao().insertAll(scores)
        }
    }

    private suspend fun refreshTransactions() {
        var items: List<Item>

        withContext(Dispatchers.IO) {
            val scores = getTrustStore().trustDao().getAll()
            // Map each database score to a UI item.
            items = scores.map { score -> TrustItem(score.publicKey, score.trustScore) }
        }

        adapterTransactions.updateItems(items)
        binding.rvScores.setItemViewCacheSize(items.size)
    }
}
