package nl.tudelft.trustchain.atomicswap.swap.eth

import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.sha256
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Convert
import java.math.BigInteger
import kotlin.random.Random

sealed class EthSwapInfo {
    abstract val amount : String
    abstract val relativeLock : Int

    /**
     * The creator is the person who locks the funds in the contract.
     */
    data class CreatorInfo(
        val recipientAddress: String,
        val secret: ByteArray,
        val secretHash: ByteArray,
        val txHash: ByteArray,
        override val relativeLock: Int,
        override val amount: String
    ) : EthSwapInfo()

    /**
     * The recipient is the person who can claim the locked funds.
     */
    data class RecipientInfo(
        val counterpartyAddress: String? = null,
        val hashUsed: ByteArray? = null,
        override val amount: String,
        override val relativeLock: Int
    ) : EthSwapInfo()
}

class EthereumSwap(
    web3j: Web3j,
    val credentials: Credentials,
    contractAddress : String,
    val relativeLock: Int = 6
) {

    val swapContract = AtomicSwapContract.load(contractAddress,web3j,credentials,DefaultGasProvider())


    val swapStorage = mutableMapOf<Long, EthSwapInfo>()

    /**
     * Note that the contract requires that the secret size be 32 bytes
     * @param claimAddress: Address of the recipient of the swap.
     * @param amount: Amount of eth to lock in the swap.
     * @return: tx hash. web3j can actually get a tx from its hash
     */
    fun createSwapForRecipient(
        offerId: Long,
        claimAddress: String,
        amount: String
    ): EthSwapInfo.CreatorInfo {

        val secret = Random.nextBytes(32)
        val secretHash = sha256(secret)

        val tx = swapContract.addSwap(
            credentials.address, secretHash, BigInteger.valueOf(10),
            Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger()
        ).send()


        println("""
            ETHSWAP

            ${tx.transactionHash}
        """.trimIndent())


        val swapData = EthSwapInfo.CreatorInfo(
            recipientAddress = claimAddress,
            secret = secret,
            secretHash = secretHash,
            amount = amount,
            relativeLock = relativeLock,
            // hash prob starts with "0x" or not (?)
            txHash = tx.transactionHash.removePrefix("0x").hexToBytes()
        )
        swapStorage[offerId] = swapData

        return swapData
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
     * @param hash: The hash of the secret value. While this is not necessary it helps in checking
     * that the hash and secret value correspond to each other.
     * @param secret: The secret needed to claim the swap.
     */
    fun claimSwap(offerId: Long,secret:ByteArray): TransactionReceipt {
        val swapInfo = when(val info = swapStorage[offerId]){
            is EthSwapInfo.RecipientInfo -> info
            null -> error("this trade is not known to us.")
            is EthSwapInfo.CreatorInfo -> error("only the recipient can claim.")
        }
        require(sha256(secret).contentEquals(swapInfo.hashUsed)) { "hash should match the secret" }

        return swapContract.claim(secret, swapInfo.hashUsed).send()
    }

    /**
     * Used when we are accepting an offer.
     */
    fun addInitialRecipientSwapdata(offerId: Long,amount: String) {
        swapStorage[offerId] = EthSwapInfo.RecipientInfo(
            amount = amount,
            relativeLock = relativeLock
        )
    }

    /**
     * Add the secret hash to info of the swap.
     */
    fun updateRecipientSwapdata(offerId: Long,secretHash:ByteArray){
        swapStorage[offerId] = when(val info = swapStorage[offerId]){
            is EthSwapInfo.RecipientInfo -> info.copy(hashUsed = secretHash)
            null -> error("This offer is not known to us.")
            else -> error("We are not the recipient in this trade.")
        }
    }

    /**
     * Reclaim the swap represented by [hash].
     *
     * Todo: check that we are at the reclaim height (?)
     */
    fun reclaimSwap(offerId: Long): TransactionReceipt {
//        val getSwap = getSwap(hash)
        val swapInfo = when(val info = swapStorage[offerId]){
            is EthSwapInfo.CreatorInfo -> info
            null -> error("This offer is not known to us.")
            else -> error("The recipient cannot reclaim")
        }
        return swapContract.reclaim(swapInfo.secretHash).send()
    }


}
