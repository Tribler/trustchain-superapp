package nl.tudelft.trustchain.atomicswap

import org.bitcoinj.core.Peer
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.listeners.OnTransactionBroadcastListener

class TransactionListener: OnTransactionBroadcastListener{

    fun setOnSecretRevealed(callback:(ByteArray) -> Unit){
        this.secretRevealed = callback
    }

    private lateinit var secretRevealed: (ByteArray) -> Unit

    override fun onTransaction(peer: Peer?, t: Transaction?) {
       if(t!=null){
           val input = t.getInput(0)
           val script = input.scriptSig
           val secret = script.chunks[2]
           val secretData = secret.data
           if (secretData != null) {
               secretRevealed(secretData)
           }
       }
    }

}
