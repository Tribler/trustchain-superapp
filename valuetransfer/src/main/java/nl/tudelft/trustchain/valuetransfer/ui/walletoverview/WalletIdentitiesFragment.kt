package nl.tudelft.trustchain.valuetransfer.ui.walletoverview

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.flow.map
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.community.IdentityCommunity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentWalletIdentitiesBinding
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.dialogs.IdentityDetailsDialog
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.ui.identity.IdentityItem
import nl.tudelft.trustchain.valuetransfer.ui.identity.IdentityItemRenderer

class WalletIdentitiesFragment : BaseFragment(R.layout.fragment_wallet_identities) {
    private val binding by viewBinding(FragmentWalletIdentitiesBinding::bind)

    private val adapter = ItemAdapter()

    private val items: LiveData<List<Item>> by lazy {
        identityStore.getAllIdentities().map { identities ->
            createItems(identities)
        }.asLiveData()
    }

    private val identityStore by lazy {
        IdentityStore.getInstance(requireContext())
    }

    private fun getCommunity(): IdentityCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("IdentityCommunity is not configured")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(
            IdentityItemRenderer(0, {}, {})
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvIdentityTitle.isVisible = identityStore.hasPersonalIdentity()

        binding.rvIdentities.adapter = adapter
        binding.rvIdentities.layoutManager = LinearLayoutManager(context)

        binding.tvNoPersonalIdentity.setOnClickListener {
            IdentityDetailsDialog(null, getCommunity()).show(parentFragmentManager, tag)
        }

        items.observe(
            viewLifecycleOwner,
            Observer {
                binding.tvNoPersonalIdentityExplanation.isVisible = it.isEmpty()
                binding.tvNoPersonalIdentity.isVisible = it.isEmpty()
                adapter.updateItems(it)
            }
        )
    }

    override fun onResume() {
        super.onResume()

        binding.tvIdentityTitle.isVisible = identityStore.hasPersonalIdentity()
    }

    private fun createItems(identities: List<Identity>): List<Item> {
        return identities.mapIndexed { _, identity ->
            IdentityItem(
                identity
            )
        }
    }
}
