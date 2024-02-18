package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.coin.AddressPrivateKeyPair
import nl.tudelft.trustchain.currencyii.coin.BitcoinNetworkOptions
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.coin.WalletManagerConfiguration
import nl.tudelft.trustchain.currencyii.databinding.FragmentImportKeysBinding

/**
 * A simple [Fragment] subclass.
 * Use the [ImportKeysFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ImportKeysFragment : Fragment() {
    private var _binding: FragmentImportKeysBinding? = null
    private val binding get() = _binding!!

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.importBtcAddressBtn.setOnClickListener {

            val networkRadioGroup = binding.bitcoinNetworksLayout.bitcoinNetworkRadioGroup
            val config = WalletManagerConfiguration(
                when (networkRadioGroup.checkedRadioButtonId) {
                    R.id.production_radiobutton -> BitcoinNetworkOptions.PRODUCTION
                    R.id.testnet_radiobutton -> BitcoinNetworkOptions.TEST_NET
                    R.id.regtest_radiobutton -> BitcoinNetworkOptions.REG_TEST
                    else -> {
                        Toast.makeText(
                            this.requireContext(),
                            "Please select a bitcoin network first",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                },
                null,
                AddressPrivateKeyPair(
                    binding.pkInput.text.toString(),
                    binding.skInput.text.toString()
                )
            )

            try {
                WalletManagerAndroid.Factory(this.requireContext().applicationContext)
                    .setConfiguration(config)
                    .init()
            } catch (t: Throwable) {
                Toast.makeText(
                    this.requireContext(),
                    "Something went wrong while initializing the new wallet. ${t.message ?: "No further information"}.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            binding.pkInput.setText("")
            binding.skInput.setText("")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImportKeysBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment ImportKeysFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() = ImportKeysFragment()
    }
}
