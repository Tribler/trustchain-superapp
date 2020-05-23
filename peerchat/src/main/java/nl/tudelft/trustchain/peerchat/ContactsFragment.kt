package nl.tudelft.trustchain.peerchat

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_contacts.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.databinding.FragmentContactsBinding
import nl.tudelft.trustchain.peerchat.db.PeerChatStore


class ContactsFragment : BaseFragment(R.layout.fragment_contacts) {
    private val binding by viewBinding(FragmentContactsBinding::bind)

    private val adapter = ItemAdapter()

    private val store by lazy {
        PeerChatStore.getInstance(requireContext())
    }

    private val items = MutableLiveData<List<Item>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(ContactItemRenderer {
            // TODO
        })

        items.observe(this, Observer {
            adapter.updateItems(it)
            imgEmpty.isVisible = it.isEmpty()
        })

        lifecycleScope.launchWhenCreated {
            while (isActive) {
                refreshContacts()
                delay(1000L)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))

        binding.btnAddNearby.setOnClickListener {
            // TODO
            fab.collapse()
        }

        binding.btnAddRemote.setOnClickListener {
            findNavController().navigate(R.id.action_contactsFragment_to_addRemoteFragment)
            fab.collapse()
        }
    }

    private fun refreshContacts() {
        val contacts = store.getContacts()
        items.value = contacts.map {
            ContactItem(it, null, null)
        }
    }
}
