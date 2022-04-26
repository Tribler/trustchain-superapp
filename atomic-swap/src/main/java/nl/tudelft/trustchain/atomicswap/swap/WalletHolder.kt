package nl.tudelft.trustchain.atomicswap.swap

import nl.tudelft.trustchain.atomicswap.BitcoinSwap
import nl.tudelft.trustchain.atomicswap.SwapTransactionBroadcastListener
import nl.tudelft.trustchain.atomicswap.SwapTransactionConfidenceListener
import nl.tudelft.trustchain.atomicswap.swap.eth.EthereumSwap
import nl.tudelft.trustchain.common.bitcoin.WalletService
import nl.tudelft.trustchain.common.ethereum.EthereumWeb3jWallet

object WalletHolder {
    val walletAppKit = WalletService.getGlobalWallet()
    val bitcoinWallet = walletAppKit.wallet()
    val bitcoinSwap = BitcoinSwap()
    val swapTransactionConfidenceListener = SwapTransactionConfidenceListener(6)
    val swapTransactionBroadcastListener = SwapTransactionBroadcastListener()

    lateinit var ethereumWallet: EthereumWeb3jWallet
    lateinit var ethSwap : EthereumSwap

    init {
        bitcoinWallet.addTransactionConfidenceEventListener(swapTransactionConfidenceListener)
        walletAppKit.peerGroup().addOnTransactionBroadcastListener(swapTransactionBroadcastListener)
    }
}
