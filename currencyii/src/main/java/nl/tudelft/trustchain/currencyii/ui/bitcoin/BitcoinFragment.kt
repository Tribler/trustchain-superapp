package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.bitcoin_networks.*
import kotlinx.android.synthetic.main.fragment_bitcoin.*
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.coin.*
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import org.bitcoinj.core.Coin
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.wallet.Wallet
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.*
import kotlin.Exception

const val BALANCE_THRESHOLD = "5"
/**
 * A simple [Fragment] subclass.
 * Use the [BitcoinFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BitcoinFragment :
    BaseFragment(R.layout.fragment_bitcoin),
    ImportKeyDialog.ImportKeyDialogListener {

    private var getBitcoinPressed = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initClickListeners()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        Log.i("Coin", "Resuming")
        refresh()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // TODO: Try catch not too nice.
        try {
            WalletManagerAndroid.getInstance()
        } catch (e: Exception) {
            Log.w("Coin", "Wallet Manager not initialized.")
            return
        }

        inflater.inflate(R.menu.bitcoin_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_bitcoin_blockchain_download -> {
                Log.i("Coin", "Navigating from BitcoinFragment to BlockchainDownloadFragment")
                findNavController().navigate(
                    BitcoinFragmentDirections.actionBitcoinFragmentToBlockchainDownloadFragment(
                        R.id.bitcoinFragment
                    )
                )
                true
            }
            R.id.item_bitcoin_wallet_settings -> {
                Log.i("Coin", "Navigating from BitcoinFragment to DaoImportOrCreate")
                findNavController().navigate(BitcoinFragmentDirections.actionBitcoinFragmentToDaoImportOrCreate())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initClickListeners() {
        val walletManager = WalletManagerAndroid.getInstance()
        button_copy_public_address.setOnClickListener {
            copyToClipboard(walletManager.protocolAddress().toString())
        }

        button_copy_wallet_seed.setOnClickListener {
            val seed = walletManager.toSeed()
            copyToClipboard("${seed.seed}, ${seed.creationTime}")
        }

        button_copy_bitcoin_public_key.setOnClickListener {
            copyToClipboard(walletManager.networkPublicECKeyHex())
        }

        bitcoin_refresh_swiper.setOnRefreshListener {
            this.refresh()

            @Suppress("DEPRECATION")
            Handler().postDelayed(
                {
                    try {
                        bitcoin_refresh_swiper.isRefreshing = false
                    } catch (e: Exception) {
                    }
                },
                1500
            )
        }

        // If the user has too little bitcoin, he can press the button to get more,
        // the amount that is added is hardcoded on the server somewhere.
        if (checkTooMuchBitcoin()) {
            disableGetBitcoinButton()
        } else {
            enableGetBitcoinButton()
        }
    }

    /**
     * If the balance on your wallet is higher than BALANCE_THRESHOLD than return true, otherwise false.
     * @return if the balance is too large
     */
    private fun checkTooMuchBitcoin(): Boolean {
        val walletManager = WalletManagerAndroid.getInstance()
        val balance = walletManager.kit.wallet().getBalance(Wallet.BalanceType.ESTIMATED)
        return balance.isGreaterThan(Coin.parseCoin(BALANCE_THRESHOLD))
    }

    /**
     * Disable the get BTC button, sets the color to gray and changes the onclick listener
     */
    @Suppress("DEPRECATION")
    private fun disableGetBitcoinButton() {
        add_btc.isClickable = false
        add_btc.background.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY)
        add_btc.setOnClickListener {
            Toast.makeText(this.requireContext(), "You already have enough bitcoin don't you think?", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Enable the get BTC button, set the color and the onclick listener correctly.
     */
    private fun enableGetBitcoinButton() {
        val walletManager = WalletManagerAndroid.getInstance()
        add_btc.isClickable = true
        add_btc.setOnClickListener {
            if (!getBitcoinPressed) {
                getBitcoinPressed = true

                if (!addBTC(walletManager.protocolAddress().toString())) {
                    Log.e("Coin", "The server response is failing")
                    Toast.makeText(this.requireContext(), "Something went wrong, please delete system32", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this.requireContext(), "Successfully added 10 BTC", Toast.LENGTH_SHORT).show()

                    Thread.sleep(1000)

                    Toast.makeText(this.requireContext(), "It can take up to a minute to register in your balance", Toast.LENGTH_SHORT).show()
                    this.refresh(true)
                }
            } else {
                Toast.makeText(this.requireContext(), "You are already given an amount of BTC, please wait a little longer", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refresh(animation: Boolean? = false) {
        if (animation!!) {
            bitcoin_refresh_swiper.isRefreshing = true

            @Suppress("DEPRECATION")
            Handler().postDelayed(
                {
                    try {
                        bitcoin_refresh_swiper.isRefreshing = false
                    } catch (e: Exception) {
                    }
                },
                1500
            )
        }

        if (!WalletManagerAndroid.isRunning) {
            return
        }

        val walletManager = WalletManagerAndroid.getInstance()

        walletBalance.text = walletManager.kit.wallet().balance.toFriendlyString()
        walletEstimatedBalance.text = walletManager.kit.wallet().getBalance(Wallet.BalanceType.ESTIMATED).toFriendlyString()
        chosenNetwork.text = when (walletManager.params.id) {
            NetworkParameters.ID_MAINNET -> "Production Network"
            NetworkParameters.ID_REGTEST -> "RegTest Network"
            NetworkParameters.ID_TESTNET -> "TestNet Network"
            else -> "Unknown Network selected"
        }
        val seed = walletManager.toSeed()
        walletSeed.text = "${seed.seed}, ${seed.creationTime}"
        yourPublicHex.text = walletManager.networkPublicECKeyHex()
        protocolKey.text = walletManager.protocolAddress().toString()

        if (checkTooMuchBitcoin()) {
            disableGetBitcoinButton()
        } else {
            enableGetBitcoinButton()
        }

        requireActivity().invalidateOptionsMenu()
    }

    /**
     * Add bitcoin to the wallet
     * @param address - The address where I have to send the BTC to.
     * @return Boolean - if the transaction was successful
     */
    private fun addBTC(address: String): Boolean {
        val executor: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())
        val future: Future<Boolean>

        val url = "https://$REG_TEST_FAUCET_DOMAIN/addBTC?address=$address"

        future = executor.submit(object : Callable<Boolean> {
            override fun call(): Boolean {
                val connection = URL(url).openConnection() as HttpURLConnection

                try {
                    // If it fails, check if there is enough balance available on the server
                    // Otherwise reset the bitcoin network on the server (there is only 15k BTC available).
                    // Also check if the Python server is still running!
                    Log.i("Coin", url)
                    Log.i("Coin", connection.responseMessage)
                    return connection.responseCode == 200
                } finally {
                    connection.disconnect()
                }
            }
        })

        return try {
            future.get(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        showNavBar()
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

    override fun onImport(address: String, privateKey: String) {
        if (!WalletManagerAndroid.isRunning) {
            val config = WalletManagerConfiguration(
                when (bitcoin_network_radio_group.checkedRadioButtonId) {
                    R.id.production_radiobutton -> BitcoinNetworkOptions.PRODUCTION
                    R.id.testnet_radiobutton -> BitcoinNetworkOptions.TEST_NET
                    R.id.regtest_radiobutton -> BitcoinNetworkOptions.REG_TEST
                    else -> {
                        Toast.makeText(this.requireContext(), "Please select a bitcoin network first", Toast.LENGTH_SHORT).show()
                        return
                    }
                },
                null,
                AddressPrivateKeyPair(address, privateKey)
            )

            try {
                WalletManagerAndroid.Factory(this.requireContext().applicationContext)
                    .setConfiguration(config)
                    .init()
            } catch (t: Throwable) {
                Toast.makeText(
                    this.requireContext(),
                    "Something went wrong while initializing the new wallet. ${
                    t.message
                        ?: "No further information"
                    }.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        } else {
            WalletManagerAndroid.getInstance().addKey(privateKey)
        }
    }

    override fun onImportDone() {
        this.refresh(true)

        @Suppress("DEPRECATION")
        Handler().postDelayed(
            {
                findNavController().navigate(BitcoinFragmentDirections.actionBitcoinFragmentToBlockchainDownloadFragment())
            },
            1500
        )
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(this.requireContext(), ClipboardManager::class.java)!!
        val clip = ClipData.newPlainText(text, text)
        clipboard.setPrimaryClip(clip)
    }
}
