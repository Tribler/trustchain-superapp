package nl.tudelft.trustchain.eurotoken

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_contact_list.*
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.eurotoken.databinding.FragmentContactListBinding

/**
 * A fragment representing a list of Items.
 */

class ContactsFragment : EuroTokenBaseFragment(R.layout.fragment_contact_list) {

    private val binding by viewBinding(FragmentContactListBinding::bind)

    private var columnCount = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    fun getContactItems(): List<ContactItem> {
        return eurotoken.getContactsWithLastTransactions().map { contactWithTransaction ->
            val (contact, transaction) = contactWithTransaction
            ContactItem(
                contact,
                transaction
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAddNearby.setOnClickListener {
            findNavController().navigate(R.id.action_contactsFragment_to_addNearbyFragment)
            fab.collapse()
        }

        binding.btnAddRemote.setOnClickListener {
            findNavController().navigate(R.id.action_contactsFragment_to_addRemoteFragment)
            fab.collapse()
        }


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_contact_list, container, false)

        // Set the adapter
        val rview = view.findViewById<RecyclerView>(R.id.list)
        if (rview is RecyclerView) {
            with(rview) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                /*
                val contacts = listOf(
                    ContactItem(
                        Contact(
                            "Alice",
                            AndroidCryptoProvider.generateKey().pub()
                        ),
                        Transaction(
                            "bpULdgYtjC2CW4NMveU8N6d4WW64nmZN",
                            20,
                            AndroidCryptoProvider.generateKey().pub(),
                            AndroidCryptoProvider.generateKey().pub(),
                            false,
                            Date(),
                            true,
                            true,
                            false,
                            false
                        )),
                    ContactItem(
                        Contact(
                            "Bob",
                            AndroidCryptoProvider.generateKey().pub()
                        ),
                        Transaction(
                            "LdgYtjC2CW4NMveU8N6d4WW64nmZNbpU",
                            15,
                            AndroidCryptoProvider.generateKey().pub(),
                            AndroidCryptoProvider.generateKey().pub(),
                            true,
                            Date(),
                            true,
                            true,
                            false,
                            false
                            )),
                    ContactItem(
                        Contact(
                            "Charlie",
                            AndroidCryptoProvider.generateKey().pub()
                            ),
                        Transaction(
                            "4nmZNbpULdgYtjC2CW4NMveU8N6d4WW6",
                            10,
                            AndroidCryptoProvider.generateKey().pub(),
                            AndroidCryptoProvider.generateKey().pub(),
                            true,
                            Date(),
                            true,
                            true,
                            false,
                            false
                        ))
                )
                 */
                val contacts = getContactItems()
                adapter = ContactsRecyclerAdapter(contacts)
            }
        }
        return view
    }

    companion object {

        // TODO: Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newInstance(columnCount: Int) =
            ContactsFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                }
            }
    }
}
