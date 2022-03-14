package nl.tudelft.trustchain.atomicswap

import org.bitcoinj.core.Peer
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener
import org.bitcoinj.wallet.Wallet

class TransactionMonitor(val depth: Int): TransactionConfidenceEventListener {

    val transactions : MutableList<TransactionMonitorEntry> = mutableListOf()

    val transactionsRecipient : MutableList<TransactionMonitorEntry> = mutableListOf()

    val claimedTransactions: MutableList<TransactionMonitorEntry> = mutableListOf()

    private lateinit var callback: (TransactionMonitorEntry) -> Unit

    private lateinit var recipientCallback: (TransactionMonitorEntry) -> Unit

    private lateinit var claimedCallback: (TransactionMonitorEntry) -> Unit

    fun setOnTransactionConfirmed(callback: (TransactionMonitorEntry) -> Unit) = callback.also {
        this.callback = it
    }

    fun setOnTransactionRecipientConfirmed(callback: (TransactionMonitorEntry) -> Unit) = callback.also {
        this.recipientCallback = it
    }

    fun setOnClaimedConfirmed(callback: (TransactionMonitorEntry) -> Unit) = callback.also {
        this.claimedCallback = it
    }

    override fun onTransactionConfidenceChanged(wallet: Wallet?, tx: Transaction?) {
        print("Transaction confidence")
        if(tx != null){
            for(entry in transactions) {
                if ((entry.transactionId == tx.txId.toString()) and (tx.getConfidence().depthInBlocks >= depth)) {
                    print("Transaction " + tx.txId + " has been confirmed")
                    transactions.remove(entry);
                    callback(entry)
                    return
                }
                }
            for(entry in transactionsRecipient) {
                if ((entry.transactionId == tx.txId.toString()) and (tx.getConfidence().depthInBlocks >= depth)) {
                    print("Transaction " + tx.txId + " has been confirmed")
                    transactionsRecipient.remove(entry);
                    recipientCallback(entry)
                    return
                }
            }
            for(entry in claimedTransactions) {
                if ((entry.transactionId == tx.txId.toString()) and (tx.getConfidence().depthInBlocks >= depth)) {
                    print("Transaction " + tx.txId + " has been confirmed")
                    claimedTransactions.remove(entry);
                    claimedCallback(entry)
                    return
                }
            }
            }
        }

    fun addTransactionToListener(entry: TransactionMonitorEntry){
        if (!transactions.contains(entry)){
            transactions.add(entry)
        }
    }

    fun addTransactionToRecipientListener(entry: TransactionMonitorEntry){
        if (!transactionsRecipient.contains(entry)){
            transactionsRecipient.add(entry)
        }
    }

    fun addClaimedTransactionListener(entry: TransactionMonitorEntry){
        if (!claimedTransactions.contains(entry)){
            claimedTransactions.add(entry)
        }
    }
}

class TransactionMonitorEntry(val transactionId: String, val offerId: String, val peer: nl.tudelft.ipv8.Peer)
