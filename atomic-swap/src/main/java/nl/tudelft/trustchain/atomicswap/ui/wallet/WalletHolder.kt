package nl.tudelft.trustchain.atomicswap.ui.wallet

import nl.tudelft.trustchain.atomicswap.BitcoinSwap
import nl.tudelft.trustchain.atomicswap.SwapTransactionBroadcastListener
import nl.tudelft.trustchain.atomicswap.SwapTransactionConfidenceListener
import nl.tudelft.trustchain.common.bitcoin.WalletService

object WalletHolder {
    val walletAppKit = WalletService.getGlobalWallet()
    val bitcoinWallet = walletAppKit.wallet()
    val bitcoinSwap = BitcoinSwap()
    val swapTransactionConfidenceListener = SwapTransactionConfidenceListener(6)
    val swapTransactionBroadcastListener = SwapTransactionBroadcastListener()

    init {
        bitcoinWallet.addTransactionConfidenceEventListener(swapTransactionConfidenceListener)
        walletAppKit.peerGroup().addOnTransactionBroadcastListener(swapTransactionBroadcastListener)
    }
}
