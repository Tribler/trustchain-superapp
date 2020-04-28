package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_my_daos.*
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.coin.*
import nl.tudelft.trustchain.currencyii.sharedWallet.SWJoinBlockTransactionData
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import java.io.File

/**
 * A simple [Fragment] subclass.
 * Use the [MyDAOsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MyDAOsFragment : BaseFragment(R.layout.fragment_my_daos) {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        handleDownloadNavigation()
        handleWalletNavigation()
        initMyDAOsView()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        showNavBar()
        return inflater.inflate(R.layout.fragment_my_daos, container, false)
    }

    private fun initMyDAOsView() {
        join_dao_fab.setOnClickListener {
            Log.i("Coin", "Opened DAO plus modal")
            val dialog = MyDAOsAddDialog()
            dialog.setTargetFragment(this, 0)
            dialog.show(parentFragmentManager, "Add DAO")
        }
        val sharedWalletBlocks = getCoinCommunity().fetchLatestJoinedSharedWalletBlocks()
        val publicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin().toHex()
        val adaptor =
            SharedWalletListAdapter(this, sharedWalletBlocks, publicKey, "Click to enter DAO")
        my_daos_list_view.adapter = adaptor
        my_daos_list_view.setOnItemClickListener { _, view, position, id ->
            val block = sharedWalletBlocks[position]
            val blockData = SWJoinBlockTransactionData(block.transaction).getData()
            findNavController().navigate(
                MyDAOsFragmentDirections.actionMyDAOsFragmentToSharedWalletTransaction(
                    blockData.SW_UNIQUE_ID,
                    blockData.SW_VOTING_THRESHOLD,
                    blockData.SW_ENTRANCE_FEE,
                    blockData.SW_TRUSTCHAIN_PKS.size,
                    block.calculateHash().toHex()
                )
            )
            Log.i("Coin", "Clicked: $view, $position, $id")
        }
        if (sharedWalletBlocks.isEmpty()) {
            enrolled_text.text =
                "You are currently not enrolled in any DAOs. Press the + button to join or create one."
        }
    }

    private fun handleDownloadNavigation() {
        val navController = findNavController()

        val args = MyDAOsFragmentArgs.fromBundle(requireArguments())
        if (args.showDownload) {
            navController.navigate(MyDAOsFragmentDirections.actionMyDAOsFragmentToBlockchainDownloadFragment())
        }
    }

    private fun handleWalletNavigation() {
        val navController = findNavController()

        val vWalletFileMainNet = File(
            this.requireContext().applicationContext.filesDir,
            "$MAIN_NET_WALLET_NAME.wallet"
        )

        val vWalletFileTestNet = File(
            this.requireContext().applicationContext.filesDir,
            "$TEST_NET_WALLET_NAME.wallet"
        )

        val mainNetWalletExists = vWalletFileMainNet.exists()
        val testNetWalletExists = vWalletFileTestNet.exists()
        val hasTwoWalletFiles = mainNetWalletExists && testNetWalletExists
        val hasOneWalletFile = mainNetWalletExists xor testNetWalletExists
        val hasNoWalletFiles = !mainNetWalletExists && !testNetWalletExists

        if (hasTwoWalletFiles && !WalletManagerAndroid.isInitialized()) {
            // Go to login, user has 2 wallet files and wallet manager is not initialized
            // TODO: go to screen to choose between main net and test net wallet
            Log.i("Coin", "Two wallet files exist, navigation to choice screen.")
            navController.navigate(MyDAOsFragmentDirections.actionMyDAOsFragmentToDaoLoginChoice())
        } else if (hasOneWalletFile && !WalletManagerAndroid.isInitialized()) {
            // Initialize wallet with the single wallet file that the user has stored
            val params = when (testNetWalletExists) {
                true -> BitcoinNetworkOptions.TEST_NET
                false -> BitcoinNetworkOptions.PRODUCTION
            }
            val config = WalletManagerConfiguration(params)
            WalletManagerAndroid.Factory(this.requireContext().applicationContext)
                .setConfiguration(config).init()
            Log.i("Coin", "Wallet file exists, starting wallet and going to download screen.")
            navController.navigate(MyDAOsFragmentDirections.actionMyDAOsFragmentToBlockchainDownloadFragment())
        } else if (hasNoWalletFiles) {
            // Go to login to create/import a bitcoin wallet, user has no wallet files
            // TODO: directly go to create/import wallet screen
            Log.i("Coin", "No wallet file exists, navigation to create screen.")
            navController.navigate(MyDAOsFragmentDirections.actionMyDAOsFragmentToDaoLoginChoice())
        }
    }
}
