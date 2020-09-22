package nl.tudelft.trustchain.eurotoken

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.eurotoken.dummy.DummyContent
import nl.tudelft.trustchain.eurotoken.entity.Contact
import nl.tudelft.trustchain.eurotoken.entity.Transaction
import java.time.LocalDateTime
import java.util.*

/**
 * A fragment representing a list of Items.
 */
class ContactFragment : Fragment() {

    private var columnCount = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
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
                val contacts = listOf(
                    ContactItem(
                        Contact(
                            "Alice",
                            "d4WW64nmZNbpULdgYtjC2CW4NMveU8N6"),
                        Transaction(
                            "bpULdgYtjC2CW4NMveU8N6d4WW64nmZN",
                            20,
                            "4nmZNbpULdgYtjC2CW4NMveU8N6d4WW6",
                            "4nmZNbpULdgYtjC2CW4NMveU8N6d4WW6",
                            false,
                            Date(),
                            true)),
                    ContactItem(
                        Contact(
                            "Bob",
                            "YtjC2CW4NMveU8N6d4WW64nmZNbpULdg"),
                        Transaction(
                            "LdgYtjC2CW4NMveU8N6d4WW64nmZNbpU",
                            15,
                            "4nmZNbpULdgYtjC2CW4NMveU8N6d4WW6",
                            "4nmZNbpULdgYtjC2CW4NMveU8N6d4WW6",
                            true,
                            Date(),
                            true)),
                    ContactItem(
                        Contact(
                            "Charlie",
                            "4nmZNbpULdgYtjC2CW4NMveU8N6d4WW6"),
                        Transaction(
                            "4nmZNbpULdgYtjC2CW4NMveU8N6d4WW6",
                            10,
                            "4nmZNbpULdgYtjC2CW4NMveU8N6d4WW6",
                            "4nmZNbpULdgYtjC2CW4NMveU8N6d4WW6",
                            true,
                            Date(),
                            true))
                )
                adapter = MyContactRecyclerViewAdapter(contacts)
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
            ContactFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                }
            }
    }
}
