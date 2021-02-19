package nl.tudelft.trustchain.liquidity.data

import nl.tudelft.trustchain.liquidity.service.WalletService
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin.valueOf
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet

class BitcoinLiquidityWallet(private val wallet: Wallet) : LiquidityWallet {
    override val coinName: String = "BTC"

    // TODO: Figure out who to send money to in a transaction response
    override fun initializePool(pool: LiquidityPool) =
        if (pool.wallet1 == this) {
            wallet.addCoinsReceivedEventListener { _, tx, _, _ ->
                pool.convert1To2(tx.outputSum.value.toDouble(), "me")
            }
        } else {
            wallet.addCoinsReceivedEventListener { _, tx, _, _ ->
                pool.convert2To1(tx.outputSum.value.toDouble(), "me")
            }
        }

    // TODO: Properly convert amount in double to long or the other way around
    override fun startTransaction(amount: Double, address: String) {
        wallet.sendCoins(
            SendRequest.to(
                Address.fromString(WalletService.params, address),
                valueOf(amount.toLong())
            )
        )
    }
}
