package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.common.util.concurrent.Service.State.RUNNING
import kotlinx.android.synthetic.main.fragment_bitcoin.*
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.coin.*
import nl.tudelft.ipv8.android.demo.ui.BaseFragment
import org.bitcoinj.core.Coin
import org.bitcoinj.core.ECKey


/**
 * A simple [Fragment] subclass.
 * Use the [BitcoinFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BitcoinFragment : BaseFragment(R.layout.fragment_bitcoin),
    ImportKeyDialog.ImportKeyDialogListener {

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
            R.id.item_blockchain_download_progress -> {
                Log.i("Coin", "Navigating from BitcoinFragment to BlockchainDownloadFragment")
                findNavController().navigate(BitcoinFragmentDirections.actionBitcoinFragmentToBlockchainDownloadFragment())
                true
            }
            R.id.item_blockchain_refresh -> {
                this.refresh(true)
                Log.i(
                    "Coin",
                    WalletManagerAndroid.getInstance().kit.wallet().toString(
                        true,
                        false,
                        false,
                        null
                    )
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initClickListeners() {
        show_wallet_button.setOnClickListener {
            Log.i("Coin", "Navigating from BitcoinFragment to MySharedWalletsFragment")
            findNavController().navigate(BitcoinFragmentDirections.actionBitcoinFragmentToMySharedWalletsFragment())
        }

        create_wallet_button.setOnClickListener {
            Log.i("Coin", "Navigating from BitcoinFragment to CreateSWFragment")
            findNavController().navigate(BitcoinFragmentDirections.actionBitcoinFragmentToCreateSWFragment())
        }

        search_wallet_button.setOnClickListener {
            Log.i("Coin", "Navigating from BitcoinFragment to JoinNetworkFragment")
            findNavController().navigate(BitcoinFragmentDirections.actionBitcoinFragmentToJoinNetworkFragment())
        }

        startWalletButtonExisting.setOnClickListener {
            val config = WalletManagerConfiguration(
                BitcoinNetworkOptions.TEST_NET
            )
            WalletManagerAndroid.Factory(this.requireContext().applicationContext)
                .setConfiguration(config)
                .init()
            refresh()
            Log.i("Coin", "Navigating from BitcoinFragment to BlockchainDownloadFragment")
            findNavController().navigate(BitcoinFragmentDirections.actionBitcoinFragmentToBlockchainDownloadFragment())
        }

        import_custom_keys.setOnClickListener {
            val dialog = ImportKeyDialog()
            dialog.setTargetFragment(this, 0)
            dialog.show(parentFragmentManager, "Import Key")
        }

        startWalletButtonImportDefaultKey.setOnClickListener {
            val config = WalletManagerConfiguration(
                BitcoinNetworkOptions.TEST_NET,
                SerializedDeterministicKey(
                    "spell seat genius horn argue family steel buyer spawn chef guard vast",
                    1583488954L
                )
            )

            WalletManagerAndroid.Factory(this.requireContext().applicationContext)
                .setConfiguration(config)
                .init()
            refresh()
            Log.i("Coin", "Navigating from BitcoinFragment to BlockchainDownloadFragment")
            findNavController().navigate(BitcoinFragmentDirections.actionBitcoinFragmentToBlockchainDownloadFragment())
        }

        generateRandomHexes.setOnClickListener {
            val key = ECKey()
            publicKeyHexes.setText("${publicKeyHexes.text}${System.lineSeparator()}${key.publicKeyAsHex}")
        }

        createMultisig.setOnClickListener {
            Log.i("Coin", "Coin: createMultisig clicked.")
            val walletManager = WalletManagerAndroid.getInstance()

            val myKey = walletManager.networkPublicECKeyHex()
            val lines = publicKeyHexes.text.lines()
            val value = coinValue.text.toString().toLong()
            val threshHold = threshHoldText.text.toString().toInt()

            val keys = lines.toMutableList()
            keys.add(myKey)
            keys.removeAt(0)

            Log.i("Coin", "Coin: your key: $myKey")
            Log.i("Coin", "Coin: all keys:")
            keys.forEach { key ->
                Log.i("Coin", "Coin: ${key}}")
            }
            Log.i("Coin", "Coin: value (satoshi) sending: $value")

            Log.i("Coin", "Coin: createMultisig, starting process.")
            val result = walletManager.startNewWalletProcess(
                keys,
                Coin.valueOf(value),
                threshHold
            )
            Log.i("Coin", "Coin: createMultisig, finished process.")

            Log.i("Coin", "Coin: createMultisig, transactionID = ${result.transactionId}")
            Log.i("Coin", "Coin: createMultisig, serialized = ${result.serializedTransaction}")
            multisigOutputText.setText(result.transactionId)

        }

        bitcoin_refresh_swiper.setOnRefreshListener {
            this.refresh()
            Handler().postDelayed({
                bitcoin_refresh_swiper.isRefreshing = false
            }, 1500)
        }
    }

    private fun refresh(animation: Boolean? = false) {
        if (animation!!) {
            bitcoin_refresh_swiper.isRefreshing = true
            Handler().postDelayed({
                bitcoin_refresh_swiper.isRefreshing = false
            }, 1500)
        }

        if (!WalletManagerAndroid.isRunning) {
            return
        }


        var walletManager = WalletManagerAndroid.getInstance()

        walletStatus.text = "Status: ${walletManager.kit.state()}"
        walletBalance.text =
            "Bitcoin available: ${walletManager.kit.wallet().balance.toFriendlyString()}"
        chosenNetwork.text = "Network: ${walletManager.params.id}"
        val seed = walletManager.toSeed()
        walletSeed.text = "Seed: ${seed.seed}, ${seed.creationTime}"
        yourPublicHex.text = "Public (Protocol) Key: ${walletManager.networkPublicECKeyHex()}"

        if (walletManager.kit.state() == RUNNING) {
            startWalletButtonExisting.isEnabled = false
            startWalletButtonExisting.isClickable = false
            startWalletButtonImportDefaultKey.isEnabled = false
            startWalletButtonImportDefaultKey.isClickable = false

            generateRandomHexes.isEnabled = true
            createMultisig.isEnabled = true
        }
        requireActivity().invalidateOptionsMenu()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
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

    override fun onImport(address: String, privateKey: String, testNet: Boolean) {
        val config = WalletManagerConfiguration(
            if (testNet) BitcoinNetworkOptions.TEST_NET else BitcoinNetworkOptions.PRODUCTION,
            null,
            PublicPrivateKeyPair(address, privateKey)
        )

        WalletManagerAndroid.Factory(this.requireContext().applicationContext)
            .setConfiguration(config)
            .init()
    }

    override fun onImportDone() {
        this.refresh(true)
    }

}
