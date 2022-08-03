package nl.tudelft.trustchain.atomicswap

import kotlinx.coroutines.runBlocking
import nl.tudelft.trustchain.atomicswap.swap.Trade
import nl.tudelft.trustchain.atomicswap.swap.WalletHolder
import org.bitcoinj.core.*
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptOpCodes
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet

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
    fun createSwapTransaction(trade: Trade, wallet: Wallet = WalletHolder.bitcoinWallet): Pair<Transaction,Script> {

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
        wallet.completeTx(sendRequest)

        // update Trade object
        trade.setOnTransactionCreated(sendRequest.tx.bitcoinSerialize())

        return sendRequest.tx to swapScript
    }




    /**
     * Creates the transaction that claims the money from the Atomic Swap transaction
     * @return the transaction
     */
    fun createClaimTransaction(trade: Trade, wallet : Wallet = WalletHolder.bitcoinWallet): Transaction {

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

        val key = wallet.findKeyFromPubKey(myPubKey)

        // create a transaction
        val transaction = Transaction(networkParams)
        transaction.setVersion(2)

        // add an output
        transaction.addOutput(swapTransactionAmount, wallet.currentReceiveAddress())

        // add an input
        val input = transaction.addInput(swapTransactionOutput)

        // unlock the locking script
        val originalLockScript = createSwapScript(counterpartyPubKey, myPubKey, secretHash)
        val sig = transaction.calculateSignature(0, key, originalLockScript, Transaction.SigHash.ALL, false)
        val sigscript = createClaimScript(sig, originalLockScript, secret)
        input.scriptSig = sigscript


        return transaction
    }
}
