package nl.tudelft.trustchain.atomicswap

import android.util.Log
import kotlinx.coroutines.runBlocking
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.atomicswap.swap.Trade
import nl.tudelft.trustchain.atomicswap.swap.WalletHolder
import org.bitcoinj.core.*
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptOpCodes
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet

//
//sealed class SwapData {
//    abstract val keyUsed: ByteArray?
//    abstract val amount: Coin
//    abstract val relativeLock: Int
//    abstract val counterpartyKey: ByteArray?
//    abstract val offerId: Long
//
//    /**
//     * The transaction that initiated the swap
//     */
//    abstract val initiateTx: ByteArray?
//
//    /**
//     * The transaction Id of the transaction that is meant to be claimed by the initiator of the swap.
//     */
//    abstract val claimByInitiatorTxId: ByteArray?
//
//    /**
//     * Used for storing meta-data about a swap where we are the one who can reclaim.
//     * @param keyUsed: the public key of the private key that was used by us.
//     * @param secretUsed: the secret of the hash which was used to create the swap.
//     * @param relativeLock: the number of blocks before the contract can be reclaimed.
//     * @param amount: the amount our counterparty receives.
//     */
//    data class CreatorSwapData(
//        override val keyUsed: ByteArray?,
//        val secretUsed: ByteArray?,
//        override val amount: Coin,
//        override val offerId: Long,
//        override val relativeLock: Int = 6,
//        override val counterpartyKey: ByteArray? = null,
//        override val initiateTx: ByteArray? = null,
//        override val claimByInitiatorTxId: ByteArray? = null,
//        val secretHash: ByteArray?
//    ) : SwapData() {
//
//    }
//
//    /**
//     * Used for storing meta-data about a swap where we are the one who can redeem.
//     * @param keyUsed: the Bitcoin public key that identifies us in the swap.
//     * @param hashUsed: the hash used to create the swap.
//     * @param relativeLock: the number of blocks before the contract can be reclaimed.
//     * @param amount: the amount our counterparty receives.
//     */
//    data class RecipientSwapData(
//        override val keyUsed: ByteArray,
//        override val amount: Coin,
//        override val offerId: Long,
//        override val relativeLock: Int = 6,
//        override val counterpartyKey: ByteArray? = null,
//        val hashUsed: ByteArray? = null,
//        override val initiateTx: ByteArray? = null,
//        override val claimByInitiatorTxId: ByteArray? = null
//    ) : SwapData()
//}

class BitcoinSwap {
    var relativeLock: Int = 6
        private set
    var networkParams: NetworkParameters = RegTestParams()
        private set


    constructor(relativeLock: Int = 6, networkParams: NetworkParameters = RegTestParams()) {
        this.relativeLock = relativeLock
        this.networkParams = networkParams
    }


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
    private fun createSwapScript(
        reclaimPubKey: ByteArray,
        claimPubKey: ByteArray,
        secretHash: ByteArray,
        relativeLock: Int = this.relativeLock
    ): Script = ScriptBuilder()
        .op(ScriptOpCodes.OP_IF)
        .number(relativeLock.toLong())
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

    private fun createClaimScript(sig: TransactionSignature, originalLockScript: Script, secret: ByteArray): Script =
        ScriptBuilder()
            .op(ScriptOpCodes.OP_1)
            .data(sig.encodeToBitcoin())
            .data(secret)
            .smallNum(0)
            .data(originalLockScript.program)
            .build()



    /**
     * Creates the transaction for initiating the swap process.
     * Called by Alice -> at this point she has all the information about the trade except for the transactions
     *
     * @return A pair of the transaction to be broadcast and some details of the swap.
     */

    fun startSwapTx(trade: Trade): Pair<Transaction,Script> {

        val myPubKey = trade.myPubKey
        val counterpartyPubKey = trade.counterpartyPubKey
        val secretHash = trade.secretHash

        if (myPubKey == null || counterpartyPubKey == null || secretHash == null) {
            error("Not all fields are initialized")
        }

        val swapScript = createSwapScript(
            reclaimPubKey = myPubKey,
            claimPubKey = counterpartyPubKey,
            secretHash = secretHash
        )

        val contractTx = Transaction(networkParams)

        contractTx.addOutput(
            Coin.parseCoin(trade.myAmount),
            ScriptBuilder.createP2SHOutputScript(swapScript)
        )

        val sendRequest = SendRequest.forTx(contractTx)
        WalletHolder.bitcoinWallet.completeTx(sendRequest)
        return sendRequest.tx to swapScript
    }


    fun createClaimTx(trade: Trade): Transaction {

        val myPubKey = trade.myPubKey
        val counterpartyPubKey = trade.counterpartyPubKey
        val counterpartyBitcoinTransaction = trade.counterpartyBitcoinTransaction
        val secretHash = trade.secretHash
        val secret = trade.secret

        if(myPubKey == null || counterpartyBitcoinTransaction == null || secretHash == null || counterpartyPubKey == null || secret == null){
            error("Some fields are not initialized")
        }

        val transaction = Transaction(RegTestParams.get(), counterpartyBitcoinTransaction)
        val key = WalletHolder.bitcoinWallet.findKeyFromPubKey(myPubKey)
        val prevTxOut = transaction.outputs.find {
            it.scriptPubKey.scriptType == Script.ScriptType.P2SH
        } ?: error("could not find transaction output")


        val contract = Transaction(networkParams)
        contract.setVersion(2)
        contract.addOutput(prevTxOut.value.div(10).multiply(9), WalletHolder.bitcoinWallet.currentReceiveAddress())

        val originalLockScript = createSwapScript(
            counterpartyPubKey,
            myPubKey,
            secretHash
        )

        Log.d("swapscript", "claim script: $originalLockScript")

        val input = contract.addInput(prevTxOut)

        val sig =
            contract.calculateSignature(0, key, originalLockScript, Transaction.SigHash.ALL, false)
        val sigscript = createClaimScript(sig, originalLockScript, secret)

        input.scriptSig = sigscript

        runBlocking {
            contract.inputs.first().verify(prevTxOut)
        }

        return contract
    }

    /**
     * Creates the reclaim transaction.
     *
     * Depending on who it was invoked, the transaction will either reclaim the Transaction claimable
     * by the initiator or the Transaction claimable by the acceptor.
     */
//    fun createReclaimTx(offerId: Long, wallet: Wallet): Transaction {
//        val txId = when (val data = swapStorage[offerId]) {
//            is SwapData.RecipientSwapData -> data.claimByInitiatorTxId
//            is SwapData.CreatorSwapData -> data.initiateTx
//            null -> null
//        } ?: error("cannot find the transaction for this offer")
//
//        val tx = wallet.getTransaction(Sha256Hash.wrap(txId))
//            ?: error("Could not find tx: ${txId.toHex()}")
//
//        Log.d("bitcoinswap", "attempting to reclaim tx : $txId of offer : $offerId")
//
//        val details = swapStorage[offerId] ?: error("could not fine swap data. ")
//
//        val key = wallet.findKeyFromPubKey(details.keyUsed)
//
//        val contract = Transaction(networkParams)
//        contract.setVersion(2)
//        // find the output by checking if it is P2SH
//        val prevTxOut = tx.outputs.find {
//            it.scriptPubKey.scriptType == Script.ScriptType.P2SH
//        } ?: error("could not find transaction output")
//        contract.addOutput(prevTxOut.value.div(10).multiply(9), wallet.currentReceiveAddress())
//
//        val secretHash = when (details) {
//            is SwapData.RecipientSwapData -> details.hashUsed
//            is SwapData.CreatorSwapData -> details.secretHash
//        } ?: error("could not get the secret hash")
//
//        val originalLockScript = createSwapScript(
//            details.keyUsed!!,
//            details.counterpartyKey ?: error("could not find counterparty key"),
//            secretHash
//        )
//
//        val input = contract.addInput(prevTxOut)
//        contract.inputs[0].sequenceNumber =
//            (0xFFFF0000L + details.relativeLock) xor (1 shl 31) xor (1 shl 22)
//
//
//        val sig =
//            contract.calculateSignature(0, key, originalLockScript, Transaction.SigHash.ALL, false)
//        val sigscript = ScriptBuilder()
//            .op(ScriptOpCodes.OP_1)
//            .data(sig.encodeToBitcoin())
//            .smallNum(1)
//            .data(originalLockScript.program)
//            .build()
//
//
//        input.scriptSig = sigscript
//
//        Log.d("bitcoinswap", "created reclaim tx for offer : $offerId and tx: $txId")
//
//
//        return contract
//
//    }
}
