package nl.tudelft.trustchain.atomicswap

import org.bitcoinj.core.Transaction
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener
import org.bitcoinj.wallet.Wallet

class TransactionMonitor(val depth: Int): TransactionConfidenceEventListener {

    val transactions : MutableList<String> = mutableListOf()

    override fun onTransactionConfidenceChanged(wallet: Wallet?, tx: Transaction?) {
        if(tx != null){
            if(transactions.contains(tx.txId.toString()) and (tx.getConfidence().depthInBlocks >= depth)){
                print("Transaction " + tx.txId + " has been confirmed")
                transactions.remove(tx.txId.toString());
            }
        }
    }

    fun addTransactionToListener(transaction: String){
        if(!transaction.contains(transaction)){
            transactions.add(transaction);
        }
    }
}
