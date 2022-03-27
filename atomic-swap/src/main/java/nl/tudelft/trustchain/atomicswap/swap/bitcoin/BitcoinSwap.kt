package nl.tudelft.trustchain.atomicswap

import kotlinx.coroutines.runBlocking
import nl.tudelft.trustchain.atomicswap.swap.Trade
import nl.tudelft.trustchain.atomicswap.swap.WalletHolder
import org.bitcoinj.core.*
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptOpCodes
import org.bitcoinj.wallet.SendRequest

class BitcoinSwap(
    private var relativeLock: Int = 6,
    private var networkParams: NetworkParameters = RegTestParams()
) {


    /**
     * Creates a new Atomic Swap script
     * @param reclaimPubKey: the key used by the user creating the contract. The key that can reclaim the contract
     * @param claimPubKey: the key that can claim the contract with the secret.
     * @param secretHash: the hash of the secret
     * @param relativeLock:
     */
    private fun createSwapScript(reclaimPubKey: ByteArray, claimPubKey: ByteArray, secretHash: ByteArray, relativeLock: Int = this.relativeLock): Script = ScriptBuilder()
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

    /**
     * Creates a script which unlocks the Atomic Swap script
     * @param sig: the signature of the Atomic Swap script
     * @param originalLockScript: the original script
     * @param secret: The secret whose hash is contained in the original lock script
    * */
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
     * @return A pair of the transaction to be broadcast and the pubKey script
     */
    fun createSwapTransaction(trade: Trade): Pair<Transaction,Script> {

        // extract the needed fields from the Trade object which should have been initialized at this point
        val myPubKey = trade.myPubKey
        val counterpartyPubKey = trade.counterpartyPubKey
        val secretHash = trade.secretHash
        if (myPubKey == null || counterpartyPubKey == null || secretHash == null) {
            error("Not all fields are initialized")
        }

        // create the locking script
        val swapScript = createSwapScript(
            reclaimPubKey = myPubKey,
            claimPubKey = counterpartyPubKey,
            secretHash = secretHash
        )

        // create the transaction
        val transaction = Transaction(networkParams)

        // set money to the output and lock the money with the script
        transaction.addOutput(
            Coin.parseCoin(trade.myAmount),
            ScriptBuilder.createP2SHOutputScript(swapScript)
        )

        // complete the transaction
        val sendRequest = SendRequest.forTx(transaction)
        WalletHolder.bitcoinWallet.completeTx(sendRequest)

        // update Trade object
        trade.setOnTransactionCreated(sendRequest.tx.bitcoinSerialize())

        return sendRequest.tx to swapScript
    }

    /**
     * Creates the transaction that claims the money from the Atomic Swap transaction
     * @return the transaction
     */
    fun createClaimTransaction(trade: Trade): Transaction {

        // extract the needed fields from the Trade object which should have been initialized at this point
        val myPubKey = trade.myPubKey
        val counterpartyPubKey = trade.counterpartyPubKey
        val counterpartyBitcoinTransaction = trade.counterpartyBitcoinTransaction
        val secretHash = trade.secretHash
        val secret = trade.secret
        if(myPubKey == null || counterpartyBitcoinTransaction == null || secretHash == null || counterpartyPubKey == null || secret == null){
            error("Some fields are not initialized")
        }

        // extract the needed data from the Atomic Swap transaction
        val swapTransaction = Transaction(RegTestParams.get(), counterpartyBitcoinTransaction)
        val swapTransactionOutput = swapTransaction.outputs.find {
            it.scriptPubKey.scriptType == Script.ScriptType.P2SH
        } ?: error("could not find transaction output")
        val swapTransactionAmount = swapTransactionOutput.value.div(10).multiply(9)

        val key = WalletHolder.bitcoinWallet.findKeyFromPubKey(myPubKey)

        // create a transaction
        val transaction = Transaction(networkParams)
        transaction.setVersion(2)

        // add an output
        transaction.addOutput(swapTransactionAmount, WalletHolder.bitcoinWallet.currentReceiveAddress())

        // add an input
        val input = transaction.addInput(swapTransactionOutput)

        // unlock the locking script
        val originalLockScript = createSwapScript(counterpartyPubKey, myPubKey, secretHash)
        val sig = transaction.calculateSignature(0, key, originalLockScript, Transaction.SigHash.ALL, false)
        val sigscript = createClaimScript(sig, originalLockScript, secret)
        input.scriptSig = sigscript

        // verify that the input can spend the given output
        runBlocking {
            transaction.inputs.first().verify(swapTransactionOutput)
        }

        return transaction
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
