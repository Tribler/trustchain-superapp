package nl.tudelft.trustchain.atomicswap.ui.wallet

import nl.tudelft.trustchain.atomicswap.BitcoinSwap
import nl.tudelft.trustchain.atomicswap.SwapTransactionBroadcastListener
import nl.tudelft.trustchain.atomicswap.SwapTransactionConfidenceListener
import nl.tudelft.trustchain.common.bitcoin.WalletService
import java.net.InetAddress

object WalletHolder {
    val walletAppKit = WalletService.getGlobalWallet()
    val bitcoinWallet = walletAppKit.wallet()
    val bitcoinSwap = BitcoinSwap()
    val swapTransactionConfidenceListener = SwapTransactionConfidenceListener(6)
    val swapTransactionBroadcastListener = SwapTransactionBroadcastListener()

    init {
        // phone
//         walletAppKit.peerGroup().addAddress(InetAddress.getByName("192.168.178.200"))
        // emulator
//        walletAppKit.peerGroup().addAddress(InetAddress.getByName("10.0.2.2"))

        bitcoinWallet.addTransactionConfidenceEventListener(swapTransactionConfidenceListener)
        walletAppKit.peerGroup().addOnTransactionBroadcastListener(swapTransactionBroadcastListener)
    }
}
