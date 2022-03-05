package nl.tudelft.trustchain.common.bitcoin

import org.bitcoinj.core.*
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.script.ScriptBuilder

class BitcoinMultiSigWallet(
    private val params: NetworkParameters,
    n: Int,
    keys: List<ECKey>
) {
    private var spendableOutputs = mutableListOf<TransactionOutput>()
    private var spentOutputs = mutableListOf<TransactionOutput>()
    private var outputScript = ScriptBuilder.createMultiSigOutputScript(n, keys)

    fun address(): String {
        return outputScript.pubKeys[0].publicKeyAsHex
    }

    fun balance(): Coin {
        var value = Coin.ZERO
        for (spendableOutput in spendableOutputs) {
            value = value.add(spendableOutput.value)
        }
        return value
    }

    fun deposit(value: Coin): Transaction {
        val transaction = Transaction(params)
        spendableOutputs.add(transaction.addOutput(value, outputScript))
        return transaction
    }

    fun startWithdraw(value: Coin, receiveAddress: Address): Transaction {
        val transaction = Transaction(params)

        val relayValue = Coin.valueOf(1000)
        val requiredValue = value + relayValue

        // Choose outputs (to spend) as transaction input.
        var inputValue = Coin.valueOf(0)
        val transactionInputs = mutableListOf<TransactionOutput>()
        for (spendableOutput in spendableOutputs) {
            transactionInputs.add(spendableOutput)
            inputValue = inputValue.add(spendableOutput.value)
            if (inputValue >= requiredValue) break
        }
        if (inputValue < requiredValue) throw NotEnoughBalanceException()
        val change = inputValue.minus(requiredValue)

        for (transactionInput in transactionInputs) {
            transaction.addInput(transactionInput)
            spendableOutputs.remove(transactionInput)
            spentOutputs.add(transactionInput)
        }

        // Send the value to the receive address, and the change back to the multi-sig wallet.
        transaction.addOutput(value, receiveAddress)
        if (change.isPositive) spendableOutputs.add(transaction.addOutput(change, outputScript))
        return transaction
    }

    fun endWithdraw(transaction: Transaction, signatures: List<TransactionSignature>): Transaction {
        for (transactionInput in transaction.inputs) {
            transactionInput.scriptSig = ScriptBuilder.createMultiSigInputScript(signatures)
        }
        transaction.verify()
        return transaction
    }

    fun hash(transaction: Transaction): List<Sha256Hash> {
        val transactionHashes = mutableListOf<Sha256Hash>()
        transaction.inputs.forEachIndexed { index, _ ->
            val transactionHash = transaction.hashForSignature(index, outputScript, Transaction.SigHash.ALL, false)
            transactionHashes.add(transactionHash)
        }
        return transactionHashes
    }

    class NotEnoughBalanceException() : Exception("Multi-sig wallet balance too low.")
}
