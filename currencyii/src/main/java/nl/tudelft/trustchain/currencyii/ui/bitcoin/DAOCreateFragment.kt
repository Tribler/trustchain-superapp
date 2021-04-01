package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_dao_wallet_load_form.*
import nl.tudelft.trustchain.currencyii.CurrencyIIMainActivity
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.coin.*
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.crypto.MnemonicException
import java.io.File

/**
 * A simple [Fragment] subclass.
 * Use the [DAOCreateFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DAOCreateFragment : BaseFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {

        handleFirstTimeUsage()
        initListeners()

        super.onActivityCreated(savedInstanceState)
    }

    private fun handleFirstTimeUsage() {
        val args = DAOCreateFragmentArgs.fromBundle(requireArguments())
        if (args.firstTime) {
            // If this is the fist time we launch the app
            // Hide nav bar & disable back button
            hideNavBar()
            val appCompatActivity = requireActivity() as AppCompatActivity
            appCompatActivity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            appCompatActivity.supportActionBar?.setHomeButtonEnabled(false)
            appCompatActivity.supportActionBar?.title = "First Time Setup"

            val currencyIIMainActivity = requireActivity() as CurrencyIIMainActivity
            currencyIIMainActivity.addTopLevelDestinationId(R.id.daoImportOrCreate)
        }
    }

    private fun initListeners() {
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

        // Check for errors in the seed
        val words = seed.split(" ")
        try {
            MnemonicCode.INSTANCE.check(words)
        } catch (e: MnemonicException) {
            Toast.makeText(
                this.requireContext(),
                "The mnemonic seed provided is not correct. ${
                e.message
                    ?: "No further information"
                }.",
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

        // Rename all stored wallet files that are currently stored on the device
        // Effectively the same as deleting, but safer as they are not lost
        hideStoredWallets()

        // Close the current wallet manager if there is one running, blocks thread until it is closed
        if (WalletManagerAndroid.isInitialized()) {
            WalletManagerAndroid.close()
        }

        try {
            WalletManagerAndroid.Factory(this.requireContext().applicationContext)
                .setConfiguration(config)
                .init()
        } catch (t: Throwable) {
            Toast.makeText(
                this.requireContext(),
                "Something went wrong while initializing the wallet. ${
                t.message
                    ?: "No further information"
                }.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val currencyIIMainActivity = requireActivity() as CurrencyIIMainActivity
        currencyIIMainActivity.removeTopLevelDestinationId(R.id.daoImportOrCreate)
        findNavController().navigate(
            DAOCreateFragmentDirections.actionDaoImportOrCreateToMyDAOsFragment(
                showDownload = true
            )
        )
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
        fun newInstance() = DAOCreateFragment()

        fun isPrivateKeyValid(privateKey: String): Boolean {
            return privateKey.length in 51..52 || privateKey.length == 64
        }
    }

    /**
     * This function "hides" stored wallets by renaming them.
     */
    private fun hideStoredWallets() {
        val vWalletFileMainNet = File(
            this.requireContext().applicationContext.filesDir,
            "$MAIN_NET_WALLET_NAME.wallet"
        )
        val vChainFileMainNet = File(
            this.requireContext().applicationContext.filesDir,
            "$MAIN_NET_WALLET_NAME.spvchain"
        )
        val vWalletFileTestNet = File(
            this.requireContext().applicationContext.filesDir,
            "$TEST_NET_WALLET_NAME.wallet"
        )
        val vChainFileTestNet = File(
            this.requireContext().applicationContext.filesDir,
            "$TEST_NET_WALLET_NAME.spvchain"
        )

        val fileSuffix = System.currentTimeMillis()

        if (vWalletFileMainNet.exists()) {
            vWalletFileMainNet.renameTo(
                File(
                    this.requireContext().applicationContext.filesDir,
                    "${MAIN_NET_WALLET_NAME}_backup_main_net_wallet_$fileSuffix.wallet"
                )
            )
            Log.w("Coin", "Renamed MainNet wallet file")
        }

        if (vChainFileMainNet.exists()) {
            vChainFileMainNet.renameTo(
                File(
                    this.requireContext().applicationContext.filesDir,
                    "${MAIN_NET_WALLET_NAME}_backup_main_net_spvchain_$fileSuffix.spvchain"
                )
            )
            Log.w("Coin", "Renamed MainNet chain file")
        }

        if (vWalletFileTestNet.exists()) {
            vWalletFileTestNet.renameTo(
                File(
                    this.requireContext().applicationContext.filesDir,
                    "${TEST_NET_WALLET_NAME}_backup_test_net_wallet_$fileSuffix.wallet"
                )
            )
            Log.w("Coin", "Renamed TestNet wallet file")
        }

        if (vChainFileTestNet.exists()) {
            vChainFileTestNet.renameTo(
                File(
                    this.requireContext().applicationContext.filesDir,
                    "${TEST_NET_WALLET_NAME}_backup_test_net_spvchain_$fileSuffix.spvchain"
                )
            )
            Log.w("Coin", "Renamed TestNet chain file")
        }
    }
}
