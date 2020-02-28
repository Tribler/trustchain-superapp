package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_bitcoin.*
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.ui.BaseFragment
import nl.tudelft.ipv8.util.hexToBytes

/**
 * A simple [Fragment] subclass.
 * Use the [BitcoinFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BitcoinFragment : BaseFragment(R.layout.fragment_bitcoin) {
    private var publicKeyReceiver: String = ""
    private var bitcoinPrivateKey: String = ""
    private var transactionAmount: Double = 0.0

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        button3.setOnClickListener {
            publicKeyReceiver = pk_receiver.text.toString()
            transactionAmount = tx_amount.text.toString().toDouble()
            outputTextView.text = "PK Receiver: $publicKeyReceiver, Amount: $transactionAmount"

            getCoinCommunity().sendCurrency(transactionAmount, publicKeyReceiver.hexToBytes())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bitcoin, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment bitcoinFragment.
         */
        @JvmStatic
        fun newInstance() = BitcoinFragment()
    }
}
