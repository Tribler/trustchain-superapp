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
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.bitcoin_networks.*
import kotlinx.android.synthetic.main.fragment_bitcoin.*
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.coin.AddressPrivateKeyPair
import nl.tudelft.trustchain.currencyii.coin.BitcoinNetworkOptions
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.coin.WalletManagerConfiguration
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import org.bitcoinj.core.Coin
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.wallet.Wallet

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
        } catch (e: IllegalStateException) {
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
            Handler().postDelayed(
                {
                    try {
                        bitcoin_refresh_swiper.isRefreshing = false
                    } catch (e: IllegalStateException) {
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
            add_btc.setOnClickListener {
                if (!getBitcoinPressed) {
                    getBitcoinPressed = true
                    addBTC(walletManager.protocolAddress().toString())
                } else {
                    Toast.makeText(this.requireContext(), "You are already given an amount of BTC, please wait a little longer", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkTooMuchBitcoin(): Boolean {
        val walletManager = WalletManagerAndroid.getInstance()
        return walletManager.kit.wallet().getBalance(Wallet.BalanceType.ESTIMATED).isGreaterThan(Coin.parseCoin("10"))
    }

    @Suppress("DEPRECATION")
    private fun disableGetBitcoinButton() {
        add_btc.isClickable = false
        add_btc.background.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY)
        add_btc.setOnClickListener {
            Toast.makeText(this.requireContext(), "You already have enough bitcoin don't you think?", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refresh(animation: Boolean? = false) {
        if (animation!!) {
            bitcoin_refresh_swiper.isRefreshing = true
            Handler().postDelayed(
                {
                    try {
                        bitcoin_refresh_swiper.isRefreshing = false
                    } catch (e: IllegalStateException) {
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
        when {
            walletManager.params.id === NetworkParameters.ID_MAINNET -> {
                chosenNetwork.text = "Production Network"
            }
            walletManager.params.id === NetworkParameters.ID_REGTEST -> {
                chosenNetwork.text = "RegTest Network"
            }
            walletManager.params.id === NetworkParameters.ID_TESTNET -> {
                chosenNetwork.text = "TestNet Network"
            }
            else -> {
                chosenNetwork.text = "Unknown Network selected"
            }
        }
        val seed = walletManager.toSeed()
        walletSeed.text = "${seed.seed}, ${seed.creationTime}"
        yourPublicHex.text = walletManager.networkPublicECKeyHex()
        protocolKey.text = walletManager.protocolAddress().toString()

        if (checkTooMuchBitcoin()) {
            disableGetBitcoinButton()
        }

        requireActivity().invalidateOptionsMenu()
    }

    /**
     * Add bitcoin to the wallet
     * @param address - The address where I have to send the BTC to.
     */
    private fun addBTC(address: String) {
        val queue = Volley.newRequestQueue(context)
        val url = "http://131.180.27.224:8000?address=$address"

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            {
                Log.i("Coin", "Successfully added bitcoin to $address")
                Toast.makeText(context, "Successfully added bitcoin", Toast.LENGTH_SHORT).show()
                getBitcoinPressed = false
                Thread.sleep(500)
                this.refresh(true)
            },
            { error ->
                Log.i("Coin", "Failed to add bitcoin to $address; error: $error")
                Toast.makeText(context, "Failed to add bitcoin", Toast.LENGTH_SHORT).show()
                getBitcoinPressed = false
            })

        queue.add(stringRequest)
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

    override fun onImport(address: String, privateKey: String, testNet: Int) {
        if (!WalletManagerAndroid.isRunning) {
            val config = WalletManagerConfiguration(
                when (bitcoin_network_radio_group.checkedRadioButtonId) {
                    1 -> BitcoinNetworkOptions.PRODUCTION
                    2 -> BitcoinNetworkOptions.TEST_NET
                    3 -> BitcoinNetworkOptions.REG_TEST
                    else -> {
                        Toast.makeText(
                            this.requireContext(),
                            "Please select a bitcoin network first",
                            Toast.LENGTH_SHORT
                        ).show()
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
