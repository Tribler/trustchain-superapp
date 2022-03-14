package nl.tudelft.trustchain.atomicswap

import android.util.Log
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import org.bitcoinj.core.*
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptOpCodes
import org.bitcoinj.wallet.KeyChain
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import kotlin.random.Random

sealed class SwapData {
    abstract val keyUsed: ByteArray
    abstract val amount: Coin
    abstract val relativeLock: Int
    abstract val counterpartyKey : ByteArray?
    abstract val offerId : Long
    /**
     * The transaction Id of the transaction that initiated the swap
     */
    abstract val initiateTxId: ByteArray?

    /**
     * The transaction Id of the transaction that is meant to be claimed by the initiator of the swap.
     */
    abstract val claimByInitiatorTxId: ByteArray?

    /**
     * Used for storing meta-data about a swap where we are the one who can reclaim.
     * @param keyUsed: the public key of the private key that was used by us.
     * @param secretUsed: the secret of the hash which was used to create the swap.
     * @param relativeLock: the number of blocks before the contract can be reclaimed.
     * @param amount: the amount our counterparty receives.
     */
    data class CreatorSwapData(
        override val keyUsed: ByteArray,
        val secretUsed: ByteArray,
        override val amount: Coin,
        override val offerId: Long,
        override val relativeLock: Int = 6,
        override val counterpartyKey: ByteArray? = null,
        override val initiateTxId: ByteArray? = null,
        override val claimByInitiatorTxId: ByteArray? = null,
    ) : SwapData() {
        val secretHash = Sha256Hash.hash(secretUsed)
    }

    /**
     * Used for storing meta-data about a swap where we are the one who can redeem.
     * @param keyUsed: the Bitcoin public key that identifies us in the swap.
     * @param hashUsed: the hash used to create the swap.
     * @param relativeLock: the number of blocks before the contract can be reclaimed.
     * @param amount: the amount our counterparty receives.
     */
    data class RecipientSwapData(
        override val keyUsed: ByteArray,
        override val amount: Coin,
        override val offerId: Long,
        override val relativeLock: Int = 6,
        override val counterpartyKey: ByteArray? = null,
        val hashUsed: ByteArray? = null,
        override val initiateTxId: ByteArray? = null,
        override val claimByInitiatorTxId: ByteArray? = null
    ) : SwapData()
}

/**
 * Todo: Figure out if what I'm doing is correct.
 *
 * Think of the swap in terms of Btc <-> Btc is confusing and annoying.
 */
class BitcoinSwap {
    var relativeLock: Int = 6
        private set
    var networkParams: NetworkParameters = RegTestParams()
        private set


    constructor(relativeLock: Int = 6, networkParams: NetworkParameters = RegTestParams()) {
        this.relativeLock = relativeLock
        this.networkParams = networkParams
    }
//    /**
//     * Configure the swap option.
//     *
//     * Should probably only be used on startup.
//     */
//    operator fun invoke(relativeLock: Int = this.relativeLock, networkParams: NetworkParameters = this.networkParams) {
//        this.relativeLock = relativeLock
//        this.networkParams = networkParams
//    }

    /**
     * Used to store info about swaps we are involved in.
     */
    val swapStorage = mutableMapOf<Long, SwapData>()


    /**
     * Create a new swap contract.
     *
     * Useful for initially creating the contract, or for recreating the contract when we are
     * ready to spend it.
     *
     * @param reclaimPubKey: the key used by the user creating the contract. The key that can
     * reclaim the contract
     * @param claimPubKey: the key that can claim the contract with the hash.
     */
    fun createSwapScript(
        reclaimPubKey: ByteArray,
        claimPubKey: ByteArray,
        secretHash: ByteArray,
        relativeLock: Int = this.relativeLock
    ): Script = ScriptBuilder()
        .op(ScriptOpCodes.OP_IF)
        .number(relativeLock.toLong()) // relative lock
        .op(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY)
        .op(ScriptOpCodes.OP_DROP)
        .data(reclaimPubKey)
        .op(ScriptOpCodes.OP_CHECKSIGVERIFY)
        .op(ScriptOpCodes.OP_ELSE)
        .op(ScriptOpCodes.OP_SHA256)
        .data(secretHash)
        .op(ScriptOpCodes.OP_EQUALVERIFY)
        .data(claimPubKey)
        .op(ScriptOpCodes.OP_CHECKSIGVERIFY)
        .op(ScriptOpCodes.OP_ENDIF)
        .build()


    /**
     * Creates the transaction for initiating the swap process.
     *
     *
     * @return A pair of the transaction to be broadcast and some details of the swap.
     */
    fun startSwapTx(
        offerId: Long,
        wallet: Wallet,
        claimPubKey: ByteArray,
        amount: String
    ): Pair<Transaction, SwapData.CreatorSwapData> {
        val amountInCoins = Coin.parseCoin(amount)
        val key = wallet.freshKey(KeyChain.KeyPurpose.AUTHENTICATION)
        val secret = Random.nextBytes(20)

        val swapScript = createSwapScript(
            reclaimPubKey = key.pubKey,
            claimPubKey = claimPubKey,
            secretHash = Sha256Hash.hash(secret)
        )

        val contractTx = Transaction(networkParams)

        contractTx.addOutput(amountInCoins, ScriptBuilder.createP2SHOutputScript(swapScript))

        val sendRequest = SendRequest.forTx(contractTx)


        wallet.completeTx(sendRequest)

        val swapData = SwapData.CreatorSwapData(
            keyUsed = key.pubKey,
            secretUsed = secret,
            amount = amountInCoins,
            relativeLock = relativeLock,
            initiateTxId = sendRequest.tx.txId.bytes,
            counterpartyKey = claimPubKey,
            offerId = offerId
        )

        swapStorage[offerId] = swapData

        return sendRequest.tx to swapData
    }


    /**
     * Used when we have received a reply from our offer accept
     */
    fun updateRecipientSwapData(offerId: Long, secretHash: ByteArray,counterPartyKey : ByteArray, txId: ByteArray) {
        swapStorage[offerId] = (swapStorage[offerId] as SwapData.RecipientSwapData).copy(
            hashUsed = secretHash,
            initiateTxId = txId,
            counterpartyKey = counterPartyKey
        )
    }

    /**
     * Used when we are accepting an offer.
     */
    fun addInitialRecipientSwapdata(offerId: Long,keyUsed: ByteArray,amount: String){
        swapStorage[offerId] = SwapData.RecipientSwapData(
            keyUsed = keyUsed,
            amount = Coin.parseCoin(amount),
            offerId = offerId
        )
    }

    /**
     * Creates the reclaim transaction.
     *
     * Depending on who it was invoked, the transaction will either reclaim the Transaction claimable
     * by the initiator or the Transaction claimable by the acceptor.
     */
    fun createReclaimTx(offerId: Long,wallet: Wallet): Transaction {
        val txId = when(val data = swapStorage[offerId]){
            is SwapData.RecipientSwapData -> data.claimByInitiatorTxId
            is SwapData.CreatorSwapData -> data.initiateTxId
            null -> null
        } ?: error("cannot find the transaction for this offer")

        val tx = wallet.getTransaction(Sha256Hash.wrap(txId)) ?: error("Could not find tx: ${txId.toHex()}")

        Log.d("bitcoinswap","attempting to reclaim tx : $txId of offer : $offerId")

        val details = swapStorage[offerId] ?: error("could not fine swap data. ")

        val key = wallet.findKeyFromPubKey(details.keyUsed)

        val contract = Transaction(networkParams)
        contract.setVersion(2)
        // find the output by checking if it is P2SH
        val prevTxOut =tx.outputs.find {
            it.scriptPubKey.scriptType == Script.ScriptType.P2SH
        } ?: error("could not find transaction output")
        contract.addOutput(prevTxOut.value.div(10).multiply(9), wallet.currentReceiveAddress())

        val secretHash = when(details){
            is SwapData.RecipientSwapData -> details.hashUsed
            is SwapData.CreatorSwapData -> details.secretHash
        } ?: error("could not get the secret hash")

        val originalLockScript = createSwapScript(
            details.keyUsed ,
            details.counterpartyKey?: error("could not find counterparty key"),
            secretHash
        )

        val input = contract.addInput(prevTxOut)
        contract.inputs[0].sequenceNumber = (0xFFFF0000L + details.relativeLock ) xor  (1 shl 31) xor (1 shl 22)


        val sig = contract.calculateSignature(0,key,originalLockScript,Transaction.SigHash.ALL,false)
        val sigscript = ScriptBuilder()
            .op(ScriptOpCodes.OP_1)
            .data(sig.encodeToBitcoin())
            .smallNum(1)
            .data(originalLockScript.program)
            .build()


        input.scriptSig = sigscript

        Log.d("bitcoinswap","created reclaim tx for offer : $offerId and tx: $txId")


        return contract

    }

    /**
     * Creates a swap tx for the initiator to claim.
     */
    fun createSwapTxForInitiator(offerId: Long,counterpartyKey:ByteArray, wallet: Wallet): Transaction {
        val details = when(val data = swapStorage[offerId]){
            is SwapData.RecipientSwapData -> data
            else -> error("We are not the recipient, Did you call the wrong function?")
        }

        val key = wallet.findKeyFromPubKey(details.keyUsed) ?: error("cannot get private key from pub key")// todo change this once we aren't dong btc<->btc

        val swapScript = createSwapScript(
            reclaimPubKey = key.pubKey,
            claimPubKey = counterpartyKey,
            secretHash = details.hashUsed ?: error("could not find hash")
        )

        val contractTx = Transaction(networkParams)

        contractTx.addOutput(details.amount, ScriptBuilder.createP2SHOutputScript(swapScript))

        val sendRequest = SendRequest.forTx(contractTx)
        wallet.completeTx(sendRequest)

        return sendRequest.tx

    }

    /**
     * Creates a tx that claims the contract created by the recipient.
     */
    fun createClaimTxForInitiator(offerId: Long,txId: ByteArray,wallet: Wallet): Transaction {
        val swapData = swapStorage[offerId] as SwapData.CreatorSwapData? ?: error("cannot find swap details")

//        val txId = swapData.claimByInitiatorTxId ?: error("could not find the transaction id")

        return createClaimTx(txId,swapData.secretUsed,offerId,wallet)
    }

    /**
     * Creates a tx that claims the contract created by the initiator.
     */
    fun createClaimTx(txId: ByteArray,secret:ByteArray,offerId: Long,wallet: Wallet): Transaction {
        Log.d("bitcoinswap","attempting to claim tx : ${txId.toHex()} of offer : $offerId")
        val tx = wallet.getTransaction(Sha256Hash.wrap(txId)) ?: error("Could not find tx: ${txId.toHex()}")
        val details = swapStorage[offerId] ?: error("could not fine swap data. ")

        val key = wallet.findKeyFromPubKey(details.keyUsed)

        val contract = Transaction(networkParams)
        contract.setVersion(2)
        // find the output by checking if it is P2SH
        val prevTxOut =tx.outputs.find {
            it.scriptPubKey.scriptType == Script.ScriptType.P2SH
        } ?: error("could not find transaction output")
        contract.addOutput(prevTxOut.value.div(10).multiply(9), wallet.currentReceiveAddress())

        val secretHash = when(details){
            is SwapData.RecipientSwapData -> details.hashUsed
            is SwapData.CreatorSwapData -> details.secretHash
        } ?: error("could not get the secret hash")

        val originalLockScript = createSwapScript(
            details.counterpartyKey ?: error("could not find counterparty key"),
            details.keyUsed,
            secretHash
        )
        val input = contract.addInput(prevTxOut)
        // we don't need to do this since we are claiming and not reclaiming
        //contract.inputs[0].sequenceNumber = (0xFFFF0000L + details.relativeLock) xor  (1 shl 31) xor (1 shl 22)


        val sig = contract.calculateSignature(0,key,originalLockScript,Transaction.SigHash.ALL,false)
        val sigscript = ScriptBuilder()
            .op(ScriptOpCodes.OP_1)
            .data(sig.encodeToBitcoin())
            .data(secret)
            .smallNum(0)
            .data(originalLockScript.program)
            .build()


        input.scriptSig = sigscript

        Log.d("bitcoinswap","created claim tx for offer : $offerId and tx: $txId")


        return contract
    }
}
