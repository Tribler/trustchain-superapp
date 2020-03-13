package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_create_sw.*
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.ui.BaseFragment


/**
 * A simple [Fragment] subclass.
 * Use the [CreateSWFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateSWFragment(
    override val controller: BitcoinViewController
) : BitcoinView, BaseFragment(R.layout.fragment_create_sw) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        create_sw_wallet_button.setOnClickListener {
            createSharedBitcoinWallet()
        }
    }

    private fun createSharedBitcoinWallet() {
        if (validateCreationInput()) {
            val entranceFee = entrance_fee_tf.text.toString().toDouble()
            val threshold = voting_threshold_tf.text.toString().toInt()

            getCoinCommunity().createSharedWallet(entranceFee, threshold, ByteArray(5))
            controller.showView("BitcoinFragment")
        } else {
            alert_label.text = "Entrance fee should be a double, threshold an integer, both >0"
        }
    }

    private fun validateCreationInput(): Boolean {
        val entranceFee = entrance_fee_tf.text.toString().toDoubleOrNull()
        val votingThreshold = voting_threshold_tf.text.toString().toIntOrNull()
        return entranceFee != null && entranceFee > 0
            && votingThreshold != null && votingThreshold > 0 && votingThreshold < 100
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_sw, container, false)
    }

    companion object {
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(controller: BitcoinViewController) = CreateSWFragment(controller)
    }
}
