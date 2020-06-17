package nl.tudelft.trustchain.peerchat.ui.feed

import android.bluetooth.BluetoothManager
import android.content.res.ColorStateList
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_contacts.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.R
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.peerchat.databinding.FragmentContactsBinding
import nl.tudelft.trustchain.peerchat.databinding.FragmentFeedBinding
import nl.tudelft.trustchain.peerchat.databinding.FragmentNewPostBinding
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.entity.Contact
import nl.tudelft.trustchain.peerchat.ui.conversation.ConversationFragment

@OptIn(ExperimentalCoroutinesApi::class)
class NewPostFragment : BaseFragment(R.layout.fragment_new_post) {
    private val binding by viewBinding(FragmentNewPostBinding::bind)

    private val store by lazy {
        PeerChatStore.getInstance(requireContext())
    }

    private val postRepository by lazy {
        PostRepository(getIpv8().getOverlay()!!, store)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.options_new_post, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.post -> {
                val text = binding.edtPost.text.toString()
                if (text.isBlank()) {
                    Toast.makeText(context, "Your post is empty", Toast.LENGTH_SHORT).show()
                } else {
                    val hash = arguments?.getString(ARG_HASH)?.hexToBytes()
                    if (hash != null) {
                        postRepository.createReply(hash, text)
                        Toast.makeText(context, "Your reply has been created", Toast.LENGTH_SHORT).show()
                    } else {
                        postRepository.createPost(text)
                        Toast.makeText(context, "Your post has been created", Toast.LENGTH_SHORT).show()
                    }
                    findNavController().navigateUp()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        val ARG_HASH = "hash"
    }
}
