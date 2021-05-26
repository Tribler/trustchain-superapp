package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_dao_login_choice.*
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.coin.*
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import java.io.File

/**
 * A simple [Fragment] subclass.
 * Use the [DAOLoginChoiceFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DAOLoginChoiceFragment : BaseFragment(R.layout.fragment_dao_login_choice) {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        load_production_wallet.setOnClickListener {
            loadWallet(BitcoinNetworkOptions.PRODUCTION)
        }

        load_regtest_wallet.setOnClickListener {
            loadWallet(BitcoinNetworkOptions.REG_TEST)
        }

        load_testnet_wallet.setOnClickListener {
            loadWallet(BitcoinNetworkOptions.TEST_NET)
        }
    }

    private fun loadWallet(params: BitcoinNetworkOptions) {
        // Close the current wallet manager if there is one running, blocks thread until it is closed
        if (WalletManagerAndroid.isInitialized()) {
            WalletManagerAndroid.close()
        }

        val hideWallets = arrayListOf(BitcoinNetworkOptions.TEST_NET, BitcoinNetworkOptions.PRODUCTION, BitcoinNetworkOptions.REG_TEST)
        hideWallets.remove(params)

        // Make sure to hide any other wallets that exists, when creating a new wallet
        hideWalletFiles(hideWallets)

        // Initialize wallet manager
        val config = WalletManagerConfiguration(params)
        WalletManagerAndroid
            .Factory(this.requireContext().applicationContext)
            .setConfiguration(config)
            .init()

        findNavController().navigate(
            DAOLoginChoiceFragmentDirections.actionDaoLoginChoiceToMyDAOsFragment(
                true
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        hideNavBar()

        return inflater.inflate(R.layout.fragment_dao_login_choice, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance() = DAOLoginChoiceFragment()
    }

    /**
     * This function "hides" stored wallets of a certain network type by renaming them.
     */
    private fun hideWalletFiles(walletToHide: ArrayList<BitcoinNetworkOptions>) {
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

        if (walletToHide.contains(BitcoinNetworkOptions.PRODUCTION)) {
            if (vWalletFileMainNet.exists()) {
                vWalletFileMainNet.renameTo(File(this.requireContext().applicationContext.filesDir, "${MAIN_NET_WALLET_NAME}_backup_main_net_wallet_$fileSuffix.wallet"))
            }
            if (vChainFileMainNet.exists()) {
                vChainFileMainNet.renameTo(File(this.requireContext().applicationContext.filesDir, "${MAIN_NET_WALLET_NAME}_backup_main_net_spvchain_$fileSuffix.spvchain"))
            }
            Log.w("Coin", "Renamed MainNet file")
        }
        if (walletToHide.contains(BitcoinNetworkOptions.REG_TEST)) {
            if (vWalletFileRegTest.exists()) {
                vWalletFileMainNet.renameTo(File(this.requireContext().applicationContext.filesDir, "${REG_TEST_WALLET_NAME}_backup_reg_test_wallet_$fileSuffix.wallet"))
            }
            if (vChainFileRegTest.exists()) {
                vChainFileMainNet.renameTo(File(this.requireContext().applicationContext.filesDir, "${REG_TEST_WALLET_NAME}_backup_reg_test_spvchain_$fileSuffix.spvchain"))
            }
            Log.w("Coin", "Renamed RegTest file")
        }
        if (walletToHide.contains(BitcoinNetworkOptions.TEST_NET)) {
            if (vWalletFileTestNet.exists()) {
                vWalletFileTestNet.renameTo(File(this.requireContext().applicationContext.filesDir, "${TEST_NET_WALLET_NAME}_backup_test_net_wallet_$fileSuffix.wallet"))
            }
            if (vChainFileTestNet.exists()) {
                vChainFileTestNet.renameTo(File(this.requireContext().applicationContext.filesDir, "${TEST_NET_WALLET_NAME}_backup_test_net_spvchain_$fileSuffix.spvchain"))
            }
            Log.w("Coin", "Renamed TestNet file")
        }
    }
}
