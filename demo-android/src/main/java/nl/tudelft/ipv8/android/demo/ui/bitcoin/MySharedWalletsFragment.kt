package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.ui.BaseFragment


/**
 * A simple [Fragment] subclass.
 * Use the [MySharedWalletFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MySharedWalletFragment(
    override val controller: BitcoinViewController
) : BitcoinView, BaseFragment(R.layout.fragment_my_shared_wallets) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_my_shared_wallets, container, false)
    }

    companion object {
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(controller: BitcoinViewController) = MySharedWalletFragment(controller)
    }
}
