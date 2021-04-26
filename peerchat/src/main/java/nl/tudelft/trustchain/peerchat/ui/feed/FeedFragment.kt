package nl.tudelft.trustchain.peerchat.ui.feed

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.liveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.R
import nl.tudelft.trustchain.peerchat.databinding.FragmentFeedBinding
import nl.tudelft.trustchain.peerchat.db.PeerChatStore

@OptIn(ExperimentalCoroutinesApi::class)
class FeedFragment : BaseFragment(R.layout.fragment_feed) {
    private val binding by viewBinding(FragmentFeedBinding::bind)

    private val store by lazy {
        PeerChatStore.getInstance(requireContext())
    }

    private val postRepository by lazy {
        PostRepository(getIpv8().getOverlay()!!, store)
    }

    private val adapter = ItemAdapter()

    private val items: LiveData<List<Item>> by lazy {
        liveData { emit(listOf<Item>()) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(
            PostItemRenderer(
                {
                    if (!postRepository.likePost(it.block)) {
                        Toast.makeText(requireContext(), "You already liked this post", Toast.LENGTH_SHORT)
                            .show()
                    }
                },
                {
                    val args = Bundle()
                    args.putString(NewPostFragment.ARG_HASH, it.block.calculateHash().toHex())
                    findNavController().navigate(R.id.action_feedFragment_to_newPostFragment, args)
                }
            )
        )

        lifecycleScope.launchWhenResumed {
            while (isActive) {
                // Refresh peer status periodically
                val items = postRepository.getPostsByFriends()
                adapter.updateItems(items)
                binding.imgEmpty.isVisible = items.isEmpty()
                delay(1000L)
            }
        }
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

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
        }

        items.observe(
            viewLifecycleOwner,
            Observer {
                adapter.updateItems(it)
                binding.imgEmpty.isVisible = it.isEmpty()
            }
        )
    }
}
