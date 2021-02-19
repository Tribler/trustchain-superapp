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
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.liquidity.R
import nl.tudelft.trustchain.liquidity.data.EuroTokenWallet
import nl.tudelft.trustchain.liquidity.service.WalletService
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.wallet.Wallet

class WalletFragment : BaseFragment(R.layout.fragment_pool_wallet) {
    lateinit var app: WalletAppKit

    protected val transactionRepository by lazy {
        TransactionRepository(getIpv8().getOverlay()!!, gatewayStore)
    }

    private val gatewayStore by lazy {
        GatewayStore.getInstance(requireContext())
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        println("Hello World!")
        val eurowallet = EuroTokenWallet(transactionRepository);
        super.onCreate(savedInstanceState)
        val walletDir = context?.cacheDir ?: throw Error("CacheDir not found")
        app = WalletService.createPersonalWallet(walletDir)
        val wallet = app.wallet()

        val clipboard = getSystemService(requireContext(), ClipboardManager::class.java) as ClipboardManager
        lifecycleScope.launchWhenStarted {
            bitCoinCopyButton.setOnClickListener {
                clipboard.setPrimaryClip(ClipData.newPlainText("Wallet Link", bitCoinAddress.text))
                Toast.makeText(requireContext(), "Copied key to clipboard!", Toast.LENGTH_SHORT).show()
            }
            while (isActive) {
                bitCoinAddress.text = wallet.currentReceiveAddress().toString()
                bitCoinBalance.text = "Wallet balance (confirmed): ${wallet.balance.toFriendlyString()}\nWallet balance (estimated): ${wallet.getBalance(
                    Wallet.BalanceType.ESTIMATED).toFriendlyString()}"
                euroTokenAddress.text = eurowallet.getWalletAddress()
                euroTokenBalance.text = "Wallet balance (confirmed): ${TransactionRepository.prettyAmount(eurowallet.getBalance())}"
                delay(1000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        app.stopAsync()
    }
}
