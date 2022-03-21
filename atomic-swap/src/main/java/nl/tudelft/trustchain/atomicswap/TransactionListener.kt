package nl.tudelft.trustchain.atomicswap

import android.util.Log
import nl.tudelft.ipv8.Peer
import nl.tudelft.trustchain.atomicswap.ui.wallet.WalletHolder
import org.bitcoinj.core.Address
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.listeners.OnTransactionBroadcastListener
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.Script

class TransactionListener(): OnTransactionBroadcastListener{

    fun setOnSecretRevealed(callback:(ByteArray) -> Unit){
        this.secretRevealed = callback
    }

    val watchedAddresses: MutableList<TransactionListenerEntry> = mutableListOf()

    fun addWatchedAddress(entry: TransactionListenerEntry){
        Log.d("Transaction Observer", "Added " + entry.address.toString())
        watchedAddresses.add(entry)
        WalletHolder.bitcoinWallet.addWatchedAddress(entry.address)
    }

    private lateinit var secretRevealed: (ByteArray) -> Unit

    override fun onTransaction(peer: org.bitcoinj.core.Peer?, t: Transaction?) {
        Log.d("Transaction Observer", t.toString())
        if(t!=null){

            val input = t.getInput(0)
            val script = input.scriptSig
            val secret = script.chunks[2]
            val secretData = secret.data
            if (secretData != null) {
                Log.d("Transaction Observer", "Called the callback")
                secretRevealed(secretData)
            }
        }
    }
}

class TransactionListenerEntry(val address: Address, val offerId: String, val peer: Peer)
