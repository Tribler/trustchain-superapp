package nl.tudelft.trustchain.atomicswap

import org.bitcoinj.core.Transaction
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener
import org.bitcoinj.wallet.Wallet

class SwapTransactionConfidenceListener(val depth: Int): TransactionConfidenceEventListener {

    // transactions lists
    val transactionsInitiator : MutableList<TransactionConfidenceEntry> = mutableListOf()

    val transactionsRecipient : MutableList<TransactionConfidenceEntry> = mutableListOf()

    val claimedTransactions: MutableList<TransactionConfidenceEntry> = mutableListOf()

    // callbacks
    private lateinit var initiatorCallback: (TransactionConfidenceEntry) -> Unit

    private lateinit var recipientCallback: (TransactionConfidenceEntry) -> Unit

    private lateinit var claimedCallback: (TransactionConfidenceEntry) -> Unit

    fun setOnTransactionConfirmed(callback: (TransactionConfidenceEntry) -> Unit) = callback.also {
        this.initiatorCallback = it
    }

    fun setOnTransactionRecipientConfirmed(callback: (TransactionConfidenceEntry) -> Unit) = callback.also {
        this.recipientCallback = it
    }

    fun setOnClaimedConfirmed(callback: (TransactionConfidenceEntry) -> Unit) = callback.also {
        this.claimedCallback = it
    }

    // listener
    override fun onTransactionConfidenceChanged(wallet: Wallet?, tx: Transaction?) {
        if(tx != null){
            for(entry in transactionsInitiator) {
                if ((entry.transactionId == tx.txId.toString()) and (tx.confidence.depthInBlocks >= depth)) {
                    transactionsInitiator.remove(entry);
                    initiatorCallback(entry)
                    return
                }
                }
            for(entry in transactionsRecipient) {
                if ((entry.transactionId == tx.txId.toString()) and (tx.confidence.depthInBlocks >= depth)) {
                    transactionsRecipient.remove(entry);
                    recipientCallback(entry)
                    return
                }
            }
            for(entry in claimedTransactions) {
                if ((entry.transactionId == tx.txId.toString()) and (tx.confidence.depthInBlocks >= depth)) {
                    claimedTransactions.remove(entry);
                    claimedCallback(entry)
                    return
                }
            }
            }
        }

    // add transactions
    fun addTransactionInitiator(entry: TransactionConfidenceEntry){
        if (!transactionsInitiator.contains(entry)){
            transactionsInitiator.add(entry)
        }
    }

    fun addTransactionRecipient(entry: TransactionConfidenceEntry){
        if (!transactionsRecipient.contains(entry)){
            transactionsRecipient.add(entry)
        }
    }

    fun addTransactionClaimed(entry: TransactionConfidenceEntry){
        if (!claimedTransactions.contains(entry)){
            claimedTransactions.add(entry)
        }
    }
}

// class for entries
class TransactionConfidenceEntry(val transactionId: String, val offerId: String, val peer: nl.tudelft.ipv8.Peer?)
