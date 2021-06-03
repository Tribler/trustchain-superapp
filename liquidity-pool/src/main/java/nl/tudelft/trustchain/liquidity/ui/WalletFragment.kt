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
import nl.tudelft.trustchain.liquidity.data.EuroTokenWallet
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.wallet.Wallet

class WalletFragment : BaseFragment(R.layout.fragment_pool_wallet) {
    /**
     * The wallet app kit used to get a running bitcoin wallet.
     */
    lateinit var app: WalletAppKit

    /**
     * A repository for transactions in Euro Tokens.
     */
    private val transactionRepository by lazy {
        TransactionRepository(getIpv8().getOverlay()!!, GatewayStore.getInstance(requireContext()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        app = WalletService.getGlobalWallet()
        val btcWallet = app.wallet()

        // Technically only necessary for the liquidity pool owner
        WalletService.initializePool(transactionRepository, getIpv8().myPeer.publicKey)

        val euroWallet = EuroTokenWallet(transactionRepository, getIpv8().myPeer.publicKey)

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

            while (isActive) {
                bitCoinAddress.text = btcWallet.currentReceiveAddress().toString()
                bitcoinBalance.text = getString(
                    R.string.wallet_balance_conf_est,
                    btcWallet.balance.toFriendlyString(),
                    btcWallet.getBalance(Wallet.BalanceType.ESTIMATED).toFriendlyString()
                )
                euroTokenAddress.text = euroWallet.getWalletAddress()
                euroTokenBalance.text = getString(
                    R.string.wallet_balance_conf,
                    TransactionRepository.prettyAmount(euroWallet.getBalance())
                )

                delay(1000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        app.stopAsync()
    }
}
