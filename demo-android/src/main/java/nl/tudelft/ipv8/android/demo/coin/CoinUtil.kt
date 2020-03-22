package nl.tudelft.ipv8.android.demo.coin

import org.bitcoinj.core.Coin
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.Transaction
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params

class CoinUtil {

    enum class FeePriority {
        LOW_PRIORITY,
        MEDIUM_PRIORITY,
        HIGH_PRIORITY
    }

    companion object {
        /**
         * Gets the fee estimates per KB for certain transaction priorities
         * Low priority: tx gets confirmed in 7+ blocks
         * Medium priority: tx gets confirmed in 3-6 blocks
         * High priority: tx gets confirmed in 1-2 blocks
         * Fees based on https://live.blockcypher.com/btc/ and https://live.blockcypher.com/btc-testnet/
         * @param params: Type of network you want the fee for
         * @param feePriority: The priority of your transaction (default: MEDIUM_PRIORITY)
         * @return Long: fee per KB in satoshi
         */
        fun calculateFeeWithPriority(params: NetworkParameters, feePriority: FeePriority = FeePriority.MEDIUM_PRIORITY): Long {
            val fee: Int = when (params) {
                MainNetParams.get() -> when (feePriority) {
                    FeePriority.LOW_PRIORITY -> 15000
                    FeePriority.MEDIUM_PRIORITY -> 25000
                    FeePriority.HIGH_PRIORITY -> 57000
                }
                TestNet3Params.get() -> when (feePriority) {
                    FeePriority.LOW_PRIORITY -> 15000
                    FeePriority.MEDIUM_PRIORITY -> 19000
                    FeePriority.HIGH_PRIORITY -> 19000
                }
                else -> return calculateFeeWithPriority(MainNetParams.get(), feePriority)
            }

            return fee.toLong()
        }

        fun feeForTransaction(tx: Transaction, params: NetworkParameters, feePriority: FeePriority = FeePriority.MEDIUM_PRIORITY): Long {
            val feePerKB = calculateFeeWithPriority(params, feePriority)
            val sizeInKB = tx.bitcoinSerialize().size.toFloat() / 1000.toFloat()
            val calculatedFee = sizeInKB * feePerKB
            return calculatedFee.toLong()
        }
    }

}
