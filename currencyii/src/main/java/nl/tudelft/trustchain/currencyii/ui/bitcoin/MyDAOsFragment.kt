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
        create_dao_fab.setOnClickListener {
            Log.i("Coin", "Go to create DAO from My DAOs")
            findNavController().navigate(R.id.createSWFragment)
        }
        val sharedWalletBlocks = getCoinCommunity().fetchLatestJoinedSharedWalletBlocks()
        val publicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin().toHex()
        val adapter =
            SharedWalletListAdapter(
                this,
                sharedWalletBlocks,
                publicKey,
                "Click to propose transfer"
            )
        my_daos_list_view.adapter = adapter
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

        button_create_dao.setOnClickListener {
            Log.i("Coin", "Onboarding go to create DAO")
            findNavController().navigate(R.id.createSWFragment)
        }
        button_join_daos.setOnClickListener {
            Log.i("Coin", "Onboarding go to join DAO")
            findNavController().navigate(R.id.joinNetworkFragment)
        }

        // Hide/show onboard depending on whether the user has joined shared wallets blocks
        handleOnboardingVisibility(sharedWalletBlocks.isEmpty())
    }

    private fun handleOnboardingVisibility(isOnboarding: Boolean) {
        if (isOnboarding) {
            // Show onboarding components
            not_enrolled_text.visibility = View.VISIBLE
            onboarding_buttons.visibility = View.VISIBLE

            // Hide create DAO button
            create_dao_fab.visibility = View.GONE
        } else {
            // Hide onboarding components
            not_enrolled_text.visibility = View.GONE
            onboarding_buttons.visibility = View.GONE

            // Show create DAO button
            create_dao_fab.visibility = View.VISIBLE
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

        val vWalletFileRegTest = File(
            this.requireContext().applicationContext.filesDir,
            "$REG_TEST_WALLET_NAME.wallet"
        )

        val mainNetWalletExists = vWalletFileMainNet.exists()
        val testNetWalletExists = vWalletFileTestNet.exists()
        val regTestWalletExists = vWalletFileRegTest.exists()

        val walletCount = arrayOf(mainNetWalletExists, testNetWalletExists, regTestWalletExists).size

        if (walletCount > 1 && !WalletManagerAndroid.isInitialized()) {
            // Go to login, user has 3 wallet files and wallet manager is not initialized
            Log.i("Coin", "$walletCount wallet files exist, navigation to choice screen.")
            navController.navigate(MyDAOsFragmentDirections.actionMyDAOsFragmentToDaoLoginChoice())
        } else if (walletCount == 1 && !WalletManagerAndroid.isInitialized()) {
            // Initialize wallet with the single wallet file that the user has stored
            val params = when {
                testNetWalletExists -> BitcoinNetworkOptions.TEST_NET
                mainNetWalletExists -> BitcoinNetworkOptions.PRODUCTION
                else -> BitcoinNetworkOptions.REG_TEST
            }
            val config = WalletManagerConfiguration(params)
            WalletManagerAndroid.Factory(this.requireContext().applicationContext)
                .setConfiguration(config).init()
            Log.i("Coin", "Wallet file exists, starting wallet and going to download screen.")
            navController.navigate(MyDAOsFragmentDirections.actionMyDAOsFragmentToBlockchainDownloadFragment())
        } else {
            // Go to login to create/import a bitcoin wallet, user has no wallet files
            Log.i("Coin", "No wallet file exists, navigation to create screen.")
            navController.navigate(
                MyDAOsFragmentDirections.actionMyDAOsFragmentToDaoImportOrCreate(
                    firstTime = true
                )
            )
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = JoinDAOFragment()
    }
}
