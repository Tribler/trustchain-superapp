package nl.tudelft.trustchain.liquidity.data

import org.bitcoinj.core.*
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder

class BitcoinMultiSigWallet(
    private val params: NetworkParameters,
    private val n: Int,
    private val keys: List<ECKey>
) {
    private var outputs = mutableListOf<TransactionOutput>()
    fun getOutputs(): List<TransactionOutput> {
        // TODO: read trustchain for all outputs to this multi-sig wallet
        return outputs
    }

    fun getAddress(): Script {
        return outputs[0].scriptPubKey
    }

    fun getBalance(): Coin {
        var value = Coin.ZERO
        for (output in outputs) {
            if (output.isAvailableForSpending) {
                value = value.add(output.value)
            }
        }
        return value
    }

    fun deposit(value: Coin): Transaction {
        val transaction = Transaction(params)

        val script = ScriptBuilder.createMultiSigOutputScript(n, keys)
        outputs.add(transaction.addOutput(value, script)) // TODO: add output via trustchain
        return transaction
    }

    fun startWithdraw(value: Coin, receiveAddress: Address): Transaction {
        val transaction = Transaction(params)

        // Choose outputs (to spend) as transaction input.
        transaction.addInput(outputs[0])

        // Send the value to the receive address, and the change back to the multi-sig wallet.
        transaction.addOutput(value, receiveAddress)
        // transaction.addOutput(change, script)

        return transaction
    }

    fun endWithdraw(transaction: Transaction, signatures: List<TransactionSignature>) {
        transaction.getInput(0).scriptSig = ScriptBuilder.createMultiSigInputScript(signatures)
        transaction.getInput(0).verify(outputs[0])
    }

    fun hash(transaction: Transaction): Sha256Hash {
        return transaction.hashForSignature(0, outputs[0].scriptPubKey, Transaction.SigHash.ALL, false)
    }
}
