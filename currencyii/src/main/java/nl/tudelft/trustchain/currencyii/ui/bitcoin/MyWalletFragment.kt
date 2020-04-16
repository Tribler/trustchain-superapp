package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.ui.BaseFragment

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MyWalletFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MyWalletFragment : BaseFragment(R.layout.fragment_my_wallet) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_my_wallet, container, false)
    }
}
