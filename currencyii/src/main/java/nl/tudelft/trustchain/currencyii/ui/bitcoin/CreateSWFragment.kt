package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.databinding.FragmentCreateSwBinding
import nl.tudelft.trustchain.currencyii.sharedWallet.SWUtil
import nl.tudelft.trustchain.currencyii.ui.BaseFragment

/**
 * A simple [Fragment] subclass.
 * Use the [CreateSWFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateSWFragment : BaseFragment(R.layout.fragment_create_sw) {

    private var _binding: FragmentCreateSwBinding? = null
    private val binding get() = _binding!!

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.createSwWalletButton.setOnClickListener {
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
                binding.alertLabel.text =
                    "Entrance fee should be an integer >= ${SWUtil.MINIMAL_TRANSACTION_AMOUNT}, threshold an integer > 0 and <= 100"
            }
            return
        }

        activity?.runOnUiThread {
            binding.alertLabel.text = "Creating wallet, this might take some time... (0%)"
        }

        val currentEntranceFee = binding.entranceFeeTf.text.toString().toLong()
        val currentThreshold = binding.votingThresholdTf.text.toString().toInt()

        activity?.runOnUiThread {
            binding.votingThresholdTf.isEnabled = false
            binding.entranceFeeTf.isEnabled = false
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
            binding.alertLabel.text = "Wallet created successfully!"
        } catch (t: Throwable) {
            enableInputFields()
            activity?.runOnUiThread {
                binding.alertLabel.text = t.message ?: "Unexpected error occurred. Try again"
            }
        }
    }

    private fun updateProgressStatus(progress: Double) {
        Log.i("Coin", "Coin: broadcast of create genesis wallet transaction progress: $progress.")

        activity?.runOnUiThread {
            if (progress >= 1) {
                binding.alertLabel?.text = "DAO creation progress: completed!"
            } else {
                val progressString = "%.0f".format(progress * 100)
                binding.alertLabel?.text = "DAO creation progress: $progressString%..."
            }
        }
    }

    private fun enableInputFields() {
        activity?.runOnUiThread {
            binding.votingThresholdTf.isEnabled = true
            binding.entranceFeeTf.isEnabled = true
        }
    }

    private fun validateCreationInput(): Boolean {
        val entranceFee = binding.entranceFeeTf.text.toString().toLongOrNull()
        val votingThreshold = binding.votingThresholdTf.text.toString().toIntOrNull()
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
    ): View {
        _binding = FragmentCreateSwBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() = CreateSWFragment()
    }
}
