package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import nl.tudelft.trustchain.currencyii.CurrencyIIMainActivity
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.coin.AddressPrivateKeyPair
import nl.tudelft.trustchain.currencyii.coin.BitcoinNetworkOptions
import nl.tudelft.trustchain.currencyii.coin.MAIN_NET_WALLET_NAME
import nl.tudelft.trustchain.currencyii.coin.REG_TEST_WALLET_NAME
import nl.tudelft.trustchain.currencyii.coin.SerializedDeterministicKey
import nl.tudelft.trustchain.currencyii.coin.TEST_NET_WALLET_NAME
import nl.tudelft.trustchain.currencyii.coin.WalletManager
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.coin.WalletManagerConfiguration
import nl.tudelft.trustchain.currencyii.databinding.FragmentDaoWalletLoadFormBinding
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
    private var _binding: FragmentDaoWalletLoadFormBinding? = null
    private val binding get() = _binding!!

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
        binding.loadWalletButton.setOnClickListener {
            loadWalletButton()
        }

        binding.generateNewSeed.setOnClickListener {
            val networkRadioGroup = binding.bitcoinNetworksLayout.bitcoinNetworkRadioGroup
            val params = when (networkRadioGroup.checkedRadioButtonId) {
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
            }
            val seed = WalletManager.generateRandomDeterministicSeed(params)
            binding.seedWordInput.setText(seed.seed)
            binding.seedNumberInput.setText(seed.creationTime.toString())
        }

        binding.loadDebugSeed.setOnClickListener {
            val seed = SerializedDeterministicKey(
                "spell seat genius horn argue family steel buyer spawn chef guard vast",
                1583488954L
            )
            binding.seedWordInput.setText(seed.seed)
            binding.seedNumberInput.setText(seed.creationTime.toString())
        }
    }

    private fun loadWalletButton() {
        val seed = binding.seedWordInput.text.toString()
        val creationNumberText = binding.seedNumberInput.text.toString()
        val privateKeys = binding.privateKeysInput.text.lines()
        val networkRadioGroup = binding.bitcoinNetworksLayout.bitcoinNetworkRadioGroup
        val params = when (networkRadioGroup.checkedRadioButtonId) {
            R.id.production_radiobutton -> BitcoinNetworkOptions.PRODUCTION
            R.id.testnet_radiobutton -> BitcoinNetworkOptions.TEST_NET
            R.id.regtest_radiobutton -> BitcoinNetworkOptions.REG_TEST
            else -> {
                Toast.makeText(
                    this.requireContext(),
                    "Please select a bitcoin network first",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
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
        _binding = FragmentDaoWalletLoadFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
        val vWalletFileRegTest = File(
            this.requireContext().applicationContext.filesDir,
            "$REG_TEST_WALLET_NAME.wallet"
        )
        val vChainFileRegTest = File(
            this.requireContext().applicationContext.filesDir,
            "$REG_TEST_WALLET_NAME.spvchain"
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

        if (vWalletFileRegTest.exists()) {
            vWalletFileRegTest.renameTo(
                File(
                    this.requireContext().applicationContext.filesDir,
                    "${REG_TEST_WALLET_NAME}_backup_reg_test_wallet_$fileSuffix.wallet"
                )
            )
            Log.w("Coin", "Renamed RegTest wallet file")
        }

        if (vChainFileRegTest.exists()) {
            vChainFileRegTest.renameTo(
                File(
                    this.requireContext().applicationContext.filesDir,
                    "${REG_TEST_WALLET_NAME}_backup_reg_test_spvchain_$fileSuffix.spvchain"
                )
            )
            Log.w("Coin", "Renamed RegTest chain file")
        }
    }
}
