package nl.tudelft.trustchain.explorer.ui.users

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.explorer.R
import nl.tudelft.trustchain.explorer.databinding.FragmentUsersBinding

class UsersFragment : BaseFragment(R.layout.fragment_users) {
    private val adapter = ItemAdapter()

    private val binding by viewBinding(FragmentUsersBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(
            UserItemRenderer {
                findNavController().navigate(
                    UsersFragmentDirections.actionUsersFragmentToBlocksFragment(it.publicKey)
                )
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayout.VERTICAL
            )
        )

        loadNetworkInfo()
    }

    private fun loadNetworkInfo() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                val items = withContext(Dispatchers.IO) {
                    val users = trustchain.getUsers()

                    users.map {
                        val peerId = AndroidCryptoProvider.keyFromPublicBin(it.publicKey)
                            .keyToHash().toHex()
                        val storedBlocks = trustchain.getStoredBlockCountForUser(it.publicKey)
                        UserItem(
                            peerId,
                            it.publicKey.toHex(),
                            it.latestSequenceNumber,
                            storedBlocks
                        )
                    }
                }
                adapter.updateItems(items)

                binding.imgEmpty.isVisible = items.isEmpty()

                delay(1000)
            }
        }
    }
}
