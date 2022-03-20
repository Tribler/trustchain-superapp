package nl.tudelft.trustchain.atomicswap

import org.bitcoinj.core.Peer
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.listeners.OnTransactionBroadcastListener

class TransactionListener: OnTransactionBroadcastListener{
    override fun onTransaction(peer: Peer?, t: Transaction?) {
       if(t!=null){
           print(t.txId)
           print("Broadcasted")
       }
    }

}
