package nl.tudelft.trustchain.peerchat.ui.feed

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.R
import nl.tudelft.trustchain.peerchat.databinding.FragmentNewPostBinding
import nl.tudelft.trustchain.peerchat.db.PeerChatStore

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
                        Toast.makeText(context, "Your reply has been created", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        postRepository.createPost(text)
                        Toast.makeText(context, "Your post has been created", Toast.LENGTH_SHORT)
                            .show()
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
