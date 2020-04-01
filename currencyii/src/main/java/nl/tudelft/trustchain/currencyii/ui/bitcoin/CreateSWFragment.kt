package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_create_sw.*
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.ui.BaseFragment

/**
 * A simple [Fragment] subclass.
 * Use the [CreateSWFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateSWFragment() : BaseFragment(R.layout.fragment_create_sw) {
    private var currentTransactionId: String? = null
    private var currentThreshold: Int? = null
    private var currentEntranceFee: Long? = null

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
            currentEntranceFee = entrance_fee_tf.text.toString().toLong()
            currentThreshold = voting_threshold_tf.text.toString().toInt()
            currentTransactionId = getCoinCommunity().createGenesisSharedWallet(currentEntranceFee!!)

            voting_threshold_tf.isEnabled = false
            entrance_fee_tf.isEnabled = false

            fetchCurrentSharedWalletStatusLoop()
        } else {
            alert_label.text = "Entrance fee should be a double, threshold an integer, both >0"
        }
    }

    private fun fetchCurrentSharedWalletStatusLoop() {
        var finished = false
        alert_label.text = "Loading... This might take some time."

        while (!finished) {
            val serializedTransaction = getCoinCommunity().fetchBitcoinTransaction(currentTransactionId!!)
            if (serializedTransaction == null) {
                Thread.sleep(1_000)
                continue
            }

            getCoinCommunity().broadcastCreatedSharedWallet(serializedTransaction, currentEntranceFee!!, currentThreshold!!)
            finished = true
        }

        resetWalletInitializationValues()
    }

    private fun resetWalletInitializationValues() {
        currentTransactionId = null
        currentThreshold = null
        currentEntranceFee = null
        alert_label.text = ""
    }

    private fun validateCreationInput(): Boolean {
        val entranceFee = entrance_fee_tf.text.toString().toDoubleOrNull()
        val votingThreshold = voting_threshold_tf.text.toString().toIntOrNull()
        return entranceFee != null &&
            entranceFee > 0 &&
            votingThreshold != null &&
            votingThreshold > 0 &&
            votingThreshold <= 100
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_sw, container, false)
    }

    companion object {
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() = CreateSWFragment()
    }
}
