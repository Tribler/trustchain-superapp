package nl.tudelft.trustchain.liquidity.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_pool_wallet.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.trustchain.common.bitcoin.WalletService
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.liquidity.R
import nl.tudelft.trustchain.liquidity.data.BitcoinLiquidityWallet
import nl.tudelft.trustchain.liquidity.data.EuroTokenWallet
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.wallet.Wallet

class WalletFragment : BaseFragment(R.layout.fragment_pool_wallet) {
    /**
     * The wallet app kit used to get a running bitcoin wallet.
     */
    lateinit var app: WalletAppKit
   // lateinit var app2: WalletAppKit

    /**
     * A repository for transactions in Euro Tokens.
     */
    private val transactionRepository by lazy {
        TransactionRepository(getIpv8().getOverlay()!!, GatewayStore.getInstance(requireContext()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



  //      val params = RegTestParams.get()
        // Create the wallets for bitcoin and euro token.
//        app = WalletService.createPersonalWallet(walletDir)
        app = WalletService.getGlobalWallet()
        // Get the directory where wallets can be stored.
//        val walletDir = context?.cacheDir ?: throw Error("CacheDir not found")
//        app = WalletService.createPersonalWallet(walletDir)
        val btwWallet = app.wallet()

        /* No longer needed second btc wallet for testing
        app2 = WalletService.createWallet(walletDir, "Alo?")
        val btwWallet2 = app2.wallet()*/

        val btcLiqWallet = BitcoinLiquidityWallet(btwWallet, app, transactionRepository, getIpv8().myPeer.publicKey)
        btcLiqWallet.initializePool()

        val euroWallet = EuroTokenWallet(transactionRepository, getIpv8().myPeer.publicKey);

        val clipboard = getSystemService(requireContext(), ClipboardManager::class.java) as ClipboardManager

        // Initialize the button actions and update loop.
        lifecycleScope.launchWhenStarted {
            bitCoinCopyButton.setOnClickListener {
                clipboard.setPrimaryClip(ClipData.newPlainText("Wallet Link", bitCoinAddress.text))
                Toast.makeText(requireContext(), "Copied key to clipboard!", Toast.LENGTH_SHORT).show()
            }
            euroTokenCopyButton.setOnClickListener {
                clipboard.setPrimaryClip(ClipData.newPlainText("Wallet Link", euroTokenAddress.text))
                Toast.makeText(requireContext(), "Copied key to clipboard!", Toast.LENGTH_SHORT).show()
            }

            Toast.makeText(requireContext(), "droop", Toast.LENGTH_SHORT).show()

            while (isActive) {
                bitCoinAddress.text = btwWallet.currentReceiveAddress().toString()
                bitcoinBalance.text = getString(R.string.wallet_balance_conf_est,
                    btwWallet.balance.toFriendlyString(),
                    btwWallet.getBalance(Wallet.BalanceType.ESTIMATED).toFriendlyString())
/*
                bitCoinAddress2.text = btwWallet2.currentReceiveAddress().toString()
                bitcoinBalance2.text = getString(R.string.wallet_balance_conf_est,
                    btwWallet2.balance.toFriendlyString(),
                    btwWallet2.getBalance(Wallet.BalanceType.ESTIMATED).toFriendlyString())
*/
                euroTokenAddress.text = euroWallet.getWalletAddress()
                euroTokenBalance.text = getString(R.string.wallet_balance_conf,
                    TransactionRepository.prettyAmount(euroWallet.getBalance()))

                delay(1000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        app.stopAsync()
    }
}
