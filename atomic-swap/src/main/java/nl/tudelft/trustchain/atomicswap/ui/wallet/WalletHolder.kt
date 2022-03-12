package nl.tudelft.trustchain.atomicswap.ui.wallet

import nl.tudelft.trustchain.common.bitcoin.WalletService
import okhttp3.internal.wait
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.wallet.Wallet
import java.net.InetAddress

object WalletHolder {
    val walletAppKit = WalletService.getGlobalWallet()
    val bitcoinWallet = walletAppKit.wallet()

    init {
            walletAppKit.peerGroup().addAddress(InetAddress.getByName("10.0.2.2"))

    }


}
