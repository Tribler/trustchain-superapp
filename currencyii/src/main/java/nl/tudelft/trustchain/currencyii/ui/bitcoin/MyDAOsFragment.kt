package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_my_daos.*
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.ui.BaseFragment

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MyDAOsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MyDAOsFragment : BaseFragment(R.layout.fragment_my_daos) {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        my_daos_btn.setOnClickListener {
            Log.i("Coin", "Navigating from BitcoinFragment to MySharedWalletsFragment")
            findNavController().navigate(R.id.mySharedWalletsFragment)
        }

        create_dao_btn.setOnClickListener {
            Log.i("Coin", "Navigating from BitcoinFragment to CreateSWFragment")
            findNavController().navigate(R.id.createSWFragment)
        }

        join_dao_btn.setOnClickListener {
            Log.i("Coin", "Navigating from BitcoinFragment to JoinNetworkFragment")
            findNavController().navigate(R.id.joinNetworkFragment)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_my_daos, container, false)
    }

}
