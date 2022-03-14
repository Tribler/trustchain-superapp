package nl.tudelft.trustchain.atomicswap

import org.bitcoinj.core.Peer
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener
import org.bitcoinj.wallet.Wallet

class TransactionMonitor(val depth: Int): TransactionConfidenceEventListener {

    val transactions : MutableList<TransactionMonitorEntry> = mutableListOf()

    private lateinit var callback: (TransactionMonitorEntry) -> Unit

    fun setOnTransactionConfirmed(callback: (TransactionMonitorEntry) -> Unit) = callback.also {
        this.callback = it
    }

    override fun onTransactionConfidenceChanged(wallet: Wallet?, tx: Transaction?) {
        print("Transaction confidence")
        if(tx != null){
            for(entry in transactions) {
                if ((entry.transactionId == tx.txId.toString()) and (tx.getConfidence().depthInBlocks >= depth)) {
                    print("Transaction " + tx.txId + " has been confirmed")
                    transactions.remove(entry);
                    callback(entry)
                    break
                }
                }
            }
        }

    fun addTransactionToListener(entry: TransactionMonitorEntry){
        if (!transactions.contains(entry)){
            transactions.add(entry)
        }
    }
}

class TransactionMonitorEntry(val transactionId: String, val offerId: String, val peer: nl.tudelft.ipv8.Peer)
