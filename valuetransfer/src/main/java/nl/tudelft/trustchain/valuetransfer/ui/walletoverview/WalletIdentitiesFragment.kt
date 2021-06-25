package nl.tudelft.trustchain.valuetransfer.ui.walletoverview

import android.os.Bundle
import android.view.View
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
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentWalletIdentitiesBinding
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.ui.identity.IdentityItem
import nl.tudelft.trustchain.valuetransfer.ui.identity.IdentityItemRenderer

class WalletIdentitiesFragment : BaseFragment(R.layout.fragment_wallet_identities) {
    private val binding by viewBinding(FragmentWalletIdentitiesBinding::bind)

    private val adapter = ItemAdapter()

    private val items: LiveData<List<Item>> by lazy {
        store.getAllIdentities().map { identities ->
            createItems(identities)
        }.asLiveData()
    }

    private val store by lazy {
        IdentityStore.getInstance(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(
            IdentityItemRenderer(
            0,
                {

                }, {

                }
            )
        )

        items.observe(
            this,
            Observer {
                adapter.updateItems(it)
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvIdentities.adapter = adapter
        binding.rvIdentities.layoutManager = LinearLayoutManager(context)
    }

    private fun createItems(identities: List<Identity>): List<Item> {
        return identities.mapIndexed { _, identity ->
            IdentityItem(
                identity
            )
        }
    }
}
