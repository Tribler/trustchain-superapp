package nl.tudelft.trustchain.common.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import nl.tudelft.trustchain.common.databinding.FragmentVotesEntryBinding
import nl.tudelft.trustchain.common.databinding.FragmentVotesTabBinding

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
    private var _binding: FragmentVotesTabBinding? = null
    private val binding get() = _binding!!

    private lateinit var votesAdapter: VotesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVotesTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.takeIf {
            it.containsKey("voters")
        }?.apply {

            val votesList = binding.votes
            votesAdapter = VotesAdapter(
                view.context,
                requireArguments().getStringArrayList("voters")!!
            )
            votesList.adapter = votesAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
        val binding = if (convertView != null) {
            FragmentVotesEntryBinding.bind(convertView)
        } else {
            FragmentVotesEntryBinding.inflate(LayoutInflater.from(context))
        }
        val view = binding.root
        binding.voterName.text = voters[position]

        return view
    }
}
