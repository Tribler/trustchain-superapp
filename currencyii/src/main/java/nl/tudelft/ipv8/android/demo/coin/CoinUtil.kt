package nl.tudelft.ipv8.android.demo.coin

import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.Transaction
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params

class CoinUtil {

    /**
     * Low priority: transaction gets confirmed in 7+ blocks
     * Medium priority: transaction gets confirmed in 3-6 blocks
     * High priority: transaction gets confirmed in 1-2 blocks
     */
    enum class TxPriority {
        LOW_PRIORITY,
        MEDIUM_PRIORITY,
        HIGH_PRIORITY
    }

    companion object {
        /**
         * Calculates the fee estimates per KB for a given network and transaction priority
         * Low priority: transaction gets confirmed in 7+ blocks
         * Medium priority: transaction gets confirmed in 3-6 blocks
         * High priority: transaction gets confirmed in 1-2 blocks
         *
         * Fee estimates are retrieved on 22-03-202 from
         * https://live.blockcypher.com/btc/ and https://live.blockcypher.com/btc-testnet/
         *
         * @param params: Type of network you want the fee for
         * @param txPriority: The priority of your transaction (default: MEDIUM_PRIORITY)
         * @return Long: fee per KB in satoshi
         */
        fun calculateFeeWithPriority(params: NetworkParameters, txPriority: TxPriority = TxPriority.MEDIUM_PRIORITY): Long {
            val fee: Int = when (params) {
                MainNetParams.get() -> when (txPriority) {
                    TxPriority.LOW_PRIORITY -> 15000
                    TxPriority.MEDIUM_PRIORITY -> 25000
                    TxPriority.HIGH_PRIORITY -> 57000
                }
                TestNet3Params.get() -> when (txPriority) {
                    TxPriority.LOW_PRIORITY -> 15000
                    TxPriority.MEDIUM_PRIORITY -> 19000
                    TxPriority.HIGH_PRIORITY -> 19000
                }
                else -> return calculateFeeWithPriority(MainNetParams.get(), txPriority)
            }
            return fee.toLong()
        }

        /**
         * Calculates the fee of a complete transaction, using the size of the transaction.
         * The estimate is based on the network parameters and transaction priority
         *
         * @param tx: The transaction
         * @param params: The network parameters
         * @param txPriority: The priority of the transaction
         * @return fee: The estimated fee for the transaction, based on tx size, network and priority
         */
        fun calculateEstimatedTransactionFee(tx: Transaction, params: NetworkParameters, txPriority: TxPriority = TxPriority.MEDIUM_PRIORITY): Long {
            val feePerKB = calculateFeeWithPriority(params, txPriority)
            val sizeInKB = tx.bitcoinSerialize().size.toFloat() / 1000.toFloat()
            val calculatedFee = sizeInKB * feePerKB
            return calculatedFee.toLong()
        }
    }

}
