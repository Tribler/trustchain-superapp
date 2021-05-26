package nl.tudelft.trustchain.common.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import nl.tudelft.trustchain.common.R

/**
 * The adapter for showing the tab fragments in the voting fragment
 */
class TabsAdapter(fragment: Fragment, private val voters: Array<ArrayList<String>>) :
    FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        // Return a NEW fragment instance in createFragment(int)
        val fragment = TabFragment()
        fragment.arguments = Bundle().apply {
            putInt("tabPosition", position)
            putStringArrayList("voters", voters[position])
        }
        return fragment
    }
}

/**
 * The fragment for showing the votes in a list
 */
class TabFragment : Fragment() {

    private lateinit var votesAdapter: VotesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_votes_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.takeIf {
            it.containsKey("voters")
        }?.apply {

            val votesList = view.findViewById<ListView>(R.id.votes)
            votesAdapter = VotesAdapter(
                view.context,
                requireArguments().getStringArrayList("voters")!!
            )
            votesList.adapter = votesAdapter
        }
    }
}

/**
 * The adapter for showing the vote entries in the TabFragment in the list.
 */
class VotesAdapter(
    private val context: Context,
    private val voters: ArrayList<String>
) : BaseAdapter() {

    override fun getCount(): Int {
        return voters.size
    }

    override fun getItem(position: Int): Any {
        return voters[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view =
            LayoutInflater.from(context).inflate(R.layout.fragment_votes_entry, parent, false)

        view.findViewById<TextView>(R.id.voter_name).text = voters[position]

        return view
    }
}
