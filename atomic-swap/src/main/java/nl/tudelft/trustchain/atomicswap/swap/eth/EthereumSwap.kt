package nl.tudelft.trustchain.atomicswap.swap.eth

import android.annotation.SuppressLint
import android.util.Log
import nl.tudelft.ipv8.util.sha256
import nl.tudelft.ipv8.util.toHex
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Convert
import java.math.BigInteger


@SuppressLint("CheckResult")
class EthereumSwap(
    web3j: Web3j,
    val credentials: Credentials,
    contractAddress : String
) {



    val txManager = RawTransactionManager(web3j,credentials,1337 )
    val swapContract = AtomicSwapContract.load(contractAddress,web3j,txManager,DefaultGasProvider())
    /**
     * Map of callbacks that are called when a swap with a hash equal to the key is claimed.
     *
     */
    val claimCallbacks = mutableMapOf<String,ClaimCallback>()
    init {

        swapContract.swapClaimedEventFlowable(DefaultBlockParameterName.EARLIEST,DefaultBlockParameterName.LATEST)
            .subscribe { event->
                claimCallbacks.remove(event.hashValue.toHex())?.invoke(event.secret) ?: run{Log.d("ETHLOG","cb is null")}
                Log.d("ETHLOG","Eth claimed, hash: ${event.hashValue.toHex()}, amount : ${event.amount}")
            }

    }

    /**
     * Called when the swap with this hash is claimed.
     */
    fun setOnClaimed(hash: ByteArray,cb : ClaimCallback){
        // bytearrays cannot be used as keys in a map
        claimCallbacks[hash.toHex()]  = cb
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
