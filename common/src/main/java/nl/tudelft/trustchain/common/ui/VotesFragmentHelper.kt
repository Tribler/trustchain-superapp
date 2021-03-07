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
import java.text.SimpleDateFormat
import java.util.*

class TabsAdapter(fragment: Fragment, private val voters: HashMap<Int, ArrayList<String>>) :
    FragmentStateAdapter(fragment) {

    private var tabFragmentList = arrayListOf<TabFragment>()

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        // Return a NEW fragment instance in createFragment(int)
        val fragment = TabFragment()
        fragment.arguments = Bundle().apply {
            putInt("tabPosition", position)
            putStringArrayList("voters", voters[position])
        }
        tabFragmentList.add(fragment)
        return fragment
    }
}

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
            it.containsKey("tabPosition")
        }?.apply {

            val votesList = view.findViewById<ListView>(R.id.votes)
            votesAdapter = VotesAdapter(
                view.context,
                requireArguments().getStringArrayList("voters")!!,
                requireArguments().getInt(
                    "tabPosition"
                )
            )
            votesList.adapter = votesAdapter
        }
    }
}

class VotesAdapter(
    private val context: Context,
    private val voters: ArrayList<String>,
    private val tabPosition: Int
) :
    BaseAdapter() {

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

        val voter = voters[position]
        view.findViewById<TextView>(R.id.voter_name).text = voter

        if (tabPosition != 2) {
            // TODO: Remove this when actual data
            val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            val formatted = simpleDateFormat.format(Date())

            view.findViewById<TextView>(R.id.voter_time).text = formatted.toString()
        }

        return view
    }
}
