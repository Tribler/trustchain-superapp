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
 * Use the [BitcoinFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LandingBitcoinFragment : BaseFragment(R.layout.fragment_landing_bitcoin) {
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        loadInitialView()
    }

    private  fun loadInitialView() {
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.landing_bitcoin_container, BitcoinFragment.newInstance())
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_landing_bitcoin, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment bitcoinFragment.
         */
        @JvmStatic
        fun newInstance() = LandingBitcoinFragment()
    }
}
