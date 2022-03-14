package nl.tudelft.trustchain.atomicswap.ui.wallet

import nl.tudelft.trustchain.atomicswap.BitcoinSwap
import nl.tudelft.trustchain.atomicswap.TransactionMonitor
import nl.tudelft.trustchain.common.bitcoin.WalletService
import okhttp3.internal.wait
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.wallet.Wallet
import java.net.InetAddress

object WalletHolder {
    val walletAppKit = WalletService.getGlobalWallet()
    val bitcoinWallet = walletAppKit.wallet()
    val monitor = TransactionMonitor(4)
    val bitcoinSwap = BitcoinSwap()

    init {
        // phone
        walletAppKit.peerGroup().addAddress(InetAddress.getByName("192.168.178.200"))
        // emulator
        walletAppKit.peerGroup().addAddress(InetAddress.getByName("10.0.2.2"))
        bitcoinWallet.addTransactionConfidenceEventListener(monitor)
    }
}
