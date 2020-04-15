package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_bitcoin.*
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.coin.*
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import org.bitcoinj.core.Address
import org.bitcoinj.script.Script

/**
 * A simple [Fragment] subclass.
 * Use the [BitcoinFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@Suppress("DEPRECATION")
class BitcoinFragment : BaseFragment(R.layout.fragment_bitcoin),
    ImportKeyDialog.ImportKeyDialogListener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        if (!WalletManagerAndroid.isInitialized()) {
            navController.navigate(R.id.daoLoginChoice)
        }

        val args = BitcoinFragmentArgs.fromBundle(requireArguments())
        if (args.showDownload) {
            navController.navigate(BitcoinFragmentDirections.actionBitcoinFragmentToBlockchainDownloadFragment())
        }
    }

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
                findNavController().navigate(BitcoinFragmentDirections.actionBitcoinFragmentToBlockchainDownloadFragment())
                true
            }
            R.id.item_dao_overview_refresh -> {
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
            R.id.item_dao_logout -> {
                Log.i("Coin", "Logging out of current DAO user")
                findNavController().navigate(BitcoinFragmentDirections.actionBitcoinFragmentToDaoLoginChoice())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initClickListeners() {
        import_custom_keys.setOnClickListener {
            val dialog = ImportKeyDialog()
            dialog.setTargetFragment(this, 0)
            dialog.show(parentFragmentManager, "Import Key")
        }

        bitcoin_refresh_swiper.setOnRefreshListener {
            this.refresh()
            Handler().postDelayed({
                try {
                    bitcoin_refresh_swiper.isRefreshing = false
                } catch (e: IllegalStateException) {
                }

            }, 1500)
        }
    }

    private fun refresh(animation: Boolean? = false) {
        if (animation!!) {
            bitcoin_refresh_swiper.isRefreshing = true
            Handler().postDelayed({
                try {
                    bitcoin_refresh_swiper.isRefreshing = false
                } catch (e: IllegalStateException) {
                }
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
        walletSeed.setText("${seed.seed}, ${seed.creationTime}")
        yourPublicHex.text = "Public (Protocol) Key: ${walletManager.networkPublicECKeyHex()}"
        protocolKey.setText(
            Address.fromKey(
                walletManager.params,
                walletManager.protocolECKey(),
                Script.ScriptType.P2PKH
            ).toString()
        )

        requireActivity().invalidateOptionsMenu()
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

    override fun onImport(address: String, privateKey: String, testNet: Boolean) {
        if (!WalletManagerAndroid.isRunning) {
            val config = WalletManagerConfiguration(
                if (testNet) BitcoinNetworkOptions.TEST_NET else BitcoinNetworkOptions.PRODUCTION,
                null,
                AddressPrivateKeyPair(address, privateKey)
            )

            WalletManagerAndroid.Factory(this.requireContext().applicationContext)
                .setConfiguration(config)
                .init()
        } else {
            WalletManagerAndroid.getInstance().addKey(privateKey)
        }
    }

    override fun onImportDone() {
        this.refresh(true)
        Handler().postDelayed({
            findNavController().navigate(BitcoinFragmentDirections.actionBitcoinFragmentToBlockchainDownloadFragment())
        }, 1500)
    }
}
