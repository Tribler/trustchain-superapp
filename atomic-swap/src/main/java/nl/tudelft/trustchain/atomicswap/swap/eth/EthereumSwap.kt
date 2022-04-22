package nl.tudelft.trustchain.atomicswap.swap.eth

import android.annotation.SuppressLint
import android.util.Log
import io.reactivex.disposables.Disposable
import io.reactivex.plugins.RxJavaPlugins
import nl.tudelft.ipv8.util.sha256
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.atomicswap.swap.Trade
import nl.tudelft.trustchain.atomicswap.ui.swap.LOG
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.DefaultBlockParameterNumber
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Convert
import java.math.BigInteger


class EthereumSwap(
    web3j: Web3j,
    val credentials: Credentials,
    contractAddress : String,
    chainId : Long
) {



    val txManager = RawTransactionManager(web3j,credentials,chainId )
    val swapContract = AtomicSwapContract.load(contractAddress,web3j,txManager,DefaultGasProvider())
    /**
     * Map of callbacks that are called when a swap with a hash equal to the key is claimed.
     *
     */
    val claimCallbacks = mutableMapOf<String,ClaimCallback>()
    init {
        var listener : Disposable? = null
        try {
            listener = swapContract.swapClaimedEventFlowable(
                DefaultBlockParameterName.LATEST,
                DefaultBlockParameterName.LATEST
            )
                .subscribe { event ->
                    claimCallbacks.remove(event.hashValue.toHex())?.invoke(event.secret)
                        ?: run { Log.d("ETHLOG", "cb is null") }
                    Log.d(
                        "ETHLOG",
                        "Eth claimed, hash: ${event.hashValue.toHex()}, amount : ${event.amount}"
                    )
                }
        } catch (e: Exception) {
            listener?.dispose()
            Log.d(LOG,e.stackTraceToString())
        }
    }

    /**
     * Called when the swap with this hash is claimed.
     */
    fun setOnClaimed(hash: ByteArray,cb : ClaimCallback){
        // bytearrays cannot be used as keys in a map
        claimCallbacks[hash.toHex()]  = cb
    }

    fun createSwap(
        trade: Trade
    ): String{
        val counterpartyAddress = trade.counterpartyAddress ?: error("Counterparty ethereum address not set, we don't know who we are trading with")
        val secretHash = trade.secretHash ?: error("secret hash not set")
        return createSwap(counterpartyAddress,trade.myAmount,secretHash,10) //todo relative lock
    }

    /**
     * Note that the contract requires that the secret size be 32 bytes
     * @param claimAddress: Address of the recipient of the swap.
     * @param amount: Amount of eth to lock in the swap.
     * @return: tx hash. web3j can actually get a tx from its hash
     */
    fun createSwap(
        claimAddress: String,
        amount: String,
        secretHash: ByteArray,
        relativeLock : Int
    ): String {

        val tx = swapContract.addSwap(
            claimAddress, secretHash, relativeLock.toBigInteger(),
            Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger()
        ).send()

        Log.d(LOG,"receipt : $tx")

        return tx.transactionHash
    }


    /**
     * query the contract for the swap info of the given hash.
     */
    fun getSwap(hash: ByteArray): AtomicSwapContract.Swap {
        return swapContract.getSwap(hash)
            .send()
    }

    /**
     * Claim the swap represented by the hash.
     * that the hash and secret value correspond to each other.
     * @param secret: The secret needed to claim the swap.
     */
    fun claimSwap(secret: ByteArray): TransactionReceipt {
        Log.d(LOG,"CLaiming eht with secret : ${secret.toHex()}")
        require(getSwap(sha256(secret)).recipient == credentials.address){
            Log.d(LOG,"Cannot claim swap.")
        }
        return swapContract.claim(secret, sha256(secret)).send()
    }


    /**
     * Reclaim the swap represented by [hash].
     *
     * Todo: check that we are at the reclaim height (?)
     */
    fun reclaimSwap(hash:ByteArray): TransactionReceipt {
        return swapContract.reclaim(hash).send()
    }


}

/**
 * Type of callback called when a swap is claimed.
 * The parameter for the callback is the secret that was used to claim the swap.
 */
typealias ClaimCallback = (ByteArray) -> Unit
