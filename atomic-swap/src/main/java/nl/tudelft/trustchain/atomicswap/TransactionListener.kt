package nl.tudelft.trustchain.atomicswap

import nl.tudelft.trustchain.atomicswap.ui.wallet.WalletHolder
import org.bitcoinj.core.Address
import org.bitcoinj.core.Peer
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.listeners.OnTransactionBroadcastListener
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.Script

class TransactionListener: OnTransactionBroadcastListener{

    fun setOnSecretRevealed(callback:(ByteArray, String) -> Unit){
        this.secretRevealed = callback
    }

    val watchedAddresses: MutableList<TransactionListenerEntry> = mutableListOf()

    fun addWatchedAddress(entry: TransactionListenerEntry){
        watchedAddresses.add(entry)
        WalletHolder.bitcoinWallet.addWatchedAddress(entry.address)
    }

    private lateinit var secretRevealed: (ByteArray, String) -> Unit

    override fun onTransaction(peer: Peer?, t: Transaction?) {
       if(t!=null){

           val input = t.getInput(0)
           val connectedOutput = input.connectedOutput

           if(connectedOutput != null){
               val scriptPubKey: Script = connectedOutput.scriptPubKey
               val address = scriptPubKey.getToAddress(RegTestParams.get())
               for (entry in watchedAddresses){
                   if (entry.address.equals(address) ){
                       watchedAddresses.remove(entry)
                       WalletHolder.bitcoinWallet.removeWatchedAddress(entry.address)
                       val script = input.scriptSig
                       val secret = script.chunks[2]
                       val secretData = secret.data
                       if (secretData != null) {
                           secretRevealed(secretData, entry.offerId)
                       }
                       break
                   }
               }
           }
       }
    }
}

class TransactionListenerEntry(val address: Address, val offerId: String)
