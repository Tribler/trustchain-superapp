package nl.tudelft.trustchain.liquidity.data

import android.util.Log
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import org.bitcoinj.core.*
import org.bitcoinj.wallet.Wallet
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener

class BitcoinLiquidityWallet(private val wallet: Wallet, private val app: WalletAppKit, private val transactionRepository: TransactionRepository, private val publicKey: PublicKey) : LiquidityWallet {

    override val coinName: String = "BTC"

    override fun initializePool() {

        // TODO: Look into different listeners, this event is called before the transfer is verified, not sure if this will be an issue
        app.wallet().addCoinsReceivedEventListener(object : WalletCoinsReceivedEventListener {
            override fun onCoinsReceived(
                wallet: Wallet?,
                tx: Transaction?,
                prevBalance: Coin?,
                newBalance: Coin?
            ) {
                val transaction = mapOf(
                    "bitcoin_tx" to tx!!.txId.toString(),
                    "amount" to tx.getValueSentToMe(wallet).toFriendlyString()
                )
                Log.d("bitcoin_received", "Bitcoins received making a note on my chain")
                transactionRepository.trustChainCommunity.createProposalBlock("bitcoin_transfer", transaction, publicKey.keyToBin())
            }
        })
    }
}


