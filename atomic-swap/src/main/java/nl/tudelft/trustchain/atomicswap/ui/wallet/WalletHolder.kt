package nl.tudelft.trustchain.atomicswap.ui.wallet

import nl.tudelft.trustchain.atomicswap.BitcoinSwap
import nl.tudelft.trustchain.atomicswap.TransactionListener
import nl.tudelft.trustchain.atomicswap.TransactionMonitor
import nl.tudelft.trustchain.common.bitcoin.WalletService
import java.net.InetAddress

object WalletHolder {
    val walletAppKit = WalletService.getGlobalWallet()
    val bitcoinWallet = walletAppKit.wallet()
    val monitor = TransactionMonitor(6)
    val transationListener = TransactionListener()
    val bitcoinSwap = BitcoinSwap()

    init {
//        // phone
//         walletAppKit.peerGroup().addAddress(InetAddress.getByName("192.168.178.200"))
        // emulator
        walletAppKit.peerGroup().addAddress(InetAddress.getByName("10.0.2.2"))
        bitcoinWallet.addTransactionConfidenceEventListener(monitor)
        walletAppKit.peerGroup().addOnTransactionBroadcastListener(transationListener)
    }
}
