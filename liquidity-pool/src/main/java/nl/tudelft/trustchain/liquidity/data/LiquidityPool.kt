package nl.tudelft.trustchain.liquidity.data

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionConfidence

abstract class LiquidityPool(
    val wallet1: LiquidityWallet,
    val wallet2: LiquidityWallet
) {
    /**
     * Hashmaps containing eurotoken and btc pending
     * transactions
     */
    val pendingBtcTransactions: HashMap<Transaction, Boolean> = HashMap<Transaction, Boolean>()
  //  val pendingEurTransactions: HashMap<TrustChainBlock, Boolean> = HashMap<TrustChainBlock, Boolean>()

    /**
     * Gets the name of the liquidity pool using the coin pair it exchanges.
     */
    fun name(): String =
        "${wallet1.coinName}/${wallet2.coinName}"

    /**
     * Converts coin type 1 to coin type 2 through a transaction sending the calculated amount of coin
     * 2 to the given address.
     */
    fun convert1To2(amount1: Double, address: String) {
        wallet2.startTransaction(calculate2From1(amount1), address)
    }

    /**
     * Converts coin type 2 to coin type 1 through a transaction sending the calculated amount of coin
     * 1 to the given address.
     */
    fun convert2To1(amount2: Double, address: String) {
        wallet1.startTransaction(calculate1From2(amount2), address)
    }

    /**
     * Calculates the amount of coin 2 that is to be exchanged for the given amount of coin 1.
     */
    abstract fun calculate2From1(amount1: Double): Double

    /**
     * Calculates the amount of coin 1 that is to be exchanged for the given amount of coin 2.
     */
    abstract fun calculate1From2(amount2: Double): Double

    /**
     * Liquidity provider that wants to join a pool
     * must call this function, including his btc & eurotoken
     * transactions
     */
  /*  fun joinPool(etx: TrustChainBlock, btx: Transaction) {
        if (btx.hasConfidence()) {
            // We have received the btc transaction (and it is included in the best chain)!
            // TODO : Check if the given transaction is deep inside the blockchain to prevent double spend attacks!
            if (btx.getConfidence().confidenceType == TransactionConfidence.ConfidenceType.BUILDING) {

            }
        }
    }*/

}
