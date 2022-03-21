package nl.tudelft.trustchain.atomicswap.swap.eth

import nl.tudelft.ipv8.util.sha256
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Convert
import java.math.BigInteger
import kotlin.random.Random

class EthereumSwap(val web3j: Web3j,val credentials: Credentials, /*contractAddress: String = "todo"*/ ) {
    var swapContract : AtomicSwapContract? = null //todo change thi


    /**
     * Note that the contract requires that the secret size be 32 bytes
     */
    fun createSwap(): Pair<ByteArray, ByteArray> {
        if(swapContract == null){
            swapContract = AtomicSwapContract.deploy(web3j,credentials,DefaultGasProvider()).send()
        }

        val secret = Random.nextBytes(32)
        val secretHash = sha256(secret)

        val receipt = swapContract!!.addSwap(credentials.address,secretHash, BigInteger.valueOf(10),
            Convert.toWei("1",Convert.Unit.ETHER).toBigInteger())
            .send()

        val getSwap = getSwap(secretHash)

        println("""
            ethswapreceipt
            ${receipt.gasUsed}
        """.trimIndent())

        println("""
            ethswap

            recipient : ${getSwap.recipient}
            amount : ${getSwap.amount}
            reclaim height : ${getSwap.reclaim_height}
            sender : ${getSwap.reclaimer}

        """.trimIndent())

        return secretHash to secret
    }

    fun getSwap(hash: ByteArray): AtomicSwapContract.Swap {
        return swapContract!!.getSwap(hash)
            .send()
    }

    /**
     * Reclaim the swap represented by the hash.
     * @param hash: The hash of the secret value. While this is not necessary it helps in checking
     * that the hash and secret value correspond to each other.
     * @param secret: The secret needed to claim the swap.
     */
    fun claimSwap(hash: ByteArray,secret:ByteArray){
        require(sha256(secret).contentEquals(hash)) { "hash should match the secret" }
        val getSwap = getSwap(hash)

        println("""
            ethswap

            recipient : ${getSwap.recipient}
            amount : ${getSwap.amount}
            reclaim height : ${getSwap.reclaim_height}
            sender : ${getSwap.reclaimer}

        """.trimIndent())
        println("me:${credentials.address}")

        println("current height : ${web3j.ethBlockNumber().send().blockNumber}")
        swapContract!!.claim(secret,hash).send()
    }

    /**
     * Reclaim the swap represented by [hash].
     *
     * Todo: check that we are at the reclaim height (?)
     */
    fun reclaimSwap(hash: ByteArray){
        val getSwap = getSwap(hash)

        println("""
            ethswap

            recipient : ${getSwap.recipient}
            amount : ${getSwap.amount}
            reclaim height : ${getSwap.reclaim_height}
            sender : ${getSwap.reclaimer}

        """.trimIndent())
        println("me:${credentials.address}")

        println("current height : ${web3j.ethBlockNumber().send().blockNumber}")
        swapContract!!.reclaim(hash).send()
    }


}
