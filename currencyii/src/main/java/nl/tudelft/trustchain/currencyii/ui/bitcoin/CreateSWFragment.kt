package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_create_sw.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.sharedWallet.SWUtil
import nl.tudelft.trustchain.currencyii.ui.BaseFragment

/**
 * A simple [Fragment] subclass.
 * Use the [CreateSWFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateSWFragment() : BaseFragment(R.layout.fragment_create_sw) {
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        create_sw_wallet_button.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    createSharedBitcoinWallet()
                }
            }
        }
    }

    private fun createSharedBitcoinWallet() {
        if (!validateCreationInput()) {
            activity?.runOnUiThread {
                alert_label.text =
                    "Entrance fee should be an integer >= ${SWUtil.MINIMAL_TRANSACTION_AMOUNT}, threshold an integer > 0 and <= 100"
            }
            return
        }

        activity?.runOnUiThread {
            alert_label.text = "Creating wallet, this might take some time... (0%)"
        }

        val currentEntranceFee = entrance_fee_tf.text.toString().toLong()
        val currentThreshold = voting_threshold_tf.text.toString().toInt()

        activity?.runOnUiThread {
            voting_threshold_tf.isEnabled = false
            entrance_fee_tf.isEnabled = false
        }

        try {
            // Try to create the bitcoin DAO
            val newDAO = getCoinCommunity().createBitcoinGenesisWallet(
                currentEntranceFee,
                currentThreshold,
                requireContext()
            )
            val walletManager = WalletManagerAndroid.getInstance()
            walletManager.addNewNonceKey(newDAO.getData().SW_UNIQUE_ID, requireContext())

            enableInputFields()
            alert_label.text = "Wallet created successfully!"
        } catch (t: Throwable) {
            enableInputFields()
            activity?.runOnUiThread {
                alert_label.text = t.message ?: "Unexpected error occurred. Try again"
            }
        }
    }

    private fun updateProgressStatus(progress: Double) {
        Log.i("Coin", "Coin: broadcast of create genesis wallet transaction progress: $progress.")

        activity?.runOnUiThread {
            if (progress >= 1) {
                alert_label?.text = "DAO creation progress: completed!"
            } else {
                val progressString = "%.0f".format(progress * 100)
                alert_label?.text = "DAO creation progress: $progressString%..."
            }
        }
    }

    private fun enableInputFields() {
        activity?.runOnUiThread {
            voting_threshold_tf.isEnabled = true
            entrance_fee_tf.isEnabled = true
        }
    }

    private fun validateCreationInput(): Boolean {
        val entranceFee = entrance_fee_tf.text.toString().toLongOrNull()
        val votingThreshold = voting_threshold_tf.text.toString().toIntOrNull()
        return entranceFee != null &&
            entranceFee >= SWUtil.MINIMAL_TRANSACTION_AMOUNT &&
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
