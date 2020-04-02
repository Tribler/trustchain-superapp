package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_dao_wallet_load_form.*
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.coin.*


/**
 * A simple [Fragment] subclass.
 * Use the [DaoImportOrCreate.newInstance] factory method to
 * create an instance of this fragment.
 */
class DaoImportOrCreate : Fragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {

        load_wallet_button.setOnClickListener {
            loadWalletButton()
        }

        generate_new_seed.setOnClickListener {
            val params = when (production_testnet_input.isChecked) {
                true -> BitcoinNetworkOptions.TEST_NET
                false -> BitcoinNetworkOptions.PRODUCTION
            }
            val seed = WalletManager.generateRandomDeterministicSeed(params)
            seed_word_input.setText(seed.seed)
            seed_number_input.setText(seed.creationTime.toString())
        }

        load_debug_seed.setOnClickListener {
            val seed = SerializedDeterministicKey(
                "spell seat genius horn argue family steel buyer spawn chef guard vast",
                1583488954L
            )
            seed_word_input.setText(seed.seed)
            seed_number_input.setText(seed.creationTime.toString())
        }

        super.onActivityCreated(savedInstanceState)
    }

    private fun loadWalletButton() {
        val seed = seed_word_input.text.toString()
        val creationNumberText = seed_number_input.text.toString()
        val privateKeys = private_keys_input.text.lines()
        val params = when (production_testnet_input.isChecked) {
            true -> BitcoinNetworkOptions.TEST_NET
            false -> BitcoinNetworkOptions.PRODUCTION
        }

        // Simple validation guards.
        if (seed.isEmpty() || creationNumberText.isEmpty()) {
            Toast.makeText(
                this.requireContext(),
                "Please fill in both the seed and creation number (at least).",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (privateKeys[0].isNotEmpty() && !isPrivateKeyValid(privateKeys[0])) {
            Toast.makeText(
                this.requireContext(),
                "The private key is not formatted properly.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val creationNumber = creationNumberText.toLong()

        val config = when (privateKeys.isEmpty() || privateKeys[0] == "") {
            true -> WalletManagerConfiguration(
                params,
                SerializedDeterministicKey(seed, creationNumber),
                null
            )
            false -> WalletManagerConfiguration(
                params,
                SerializedDeterministicKey(seed, creationNumber),
                AddressPrivateKeyPair("", privateKeys[0])
            )
        }

        WalletManagerAndroid.Factory(this.requireContext().applicationContext)
            .setConfiguration(config)
            .init()

        findNavController().navigate(DaoImportOrCreateDirections.actionDaoImportOrCreateToBlockchainDownloadFragment())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dao_wallet_load_form, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance() = DaoImportOrCreate()

        fun isPrivateKeyValid(privateKey: String): Boolean {
            return privateKey.length in 51..52 || privateKey.length == 64
        }

    }
}
