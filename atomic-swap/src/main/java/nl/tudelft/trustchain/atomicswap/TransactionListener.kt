package nl.tudelft.trustchain.atomicswap

import android.util.Log
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.atomicswap.ui.wallet.WalletHolder
import org.bitcoinj.core.Address
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.listeners.OnTransactionBroadcastListener
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.Script

class TransactionListener(): OnTransactionBroadcastListener{

    fun setOnSecretRevealed(callback:(ByteArray, String) -> Unit){
        this.secretRevealed = callback
    }

    val watchedAddresses: MutableList<TransactionListenerEntry> = mutableListOf()

    fun addWatchedAddress(entry: TransactionListenerEntry){
        watchedAddresses.add(entry)
        WalletHolder.bitcoinWallet.addWatchedAddress(entry.address)
    }

    private lateinit var secretRevealed: (ByteArray, String) -> Unit

    override fun onTransaction(peer: org.bitcoinj.core.Peer?, t: Transaction?) {
        if(t!=null){

            val input = t.getInput(0)
            val script = input.scriptSig
            val secret = script.chunks[2]
            val secretData = secret.data
            val originalLockScript = script.chunks[4].data
            if(originalLockScript != null) {
                Log.d("Transaction Observer 2", originalLockScript.toHex())
                for(entry in watchedAddresses){
                    if(entry.script.program.contentEquals(originalLockScript)){
                        Log.d("Transaction Observer", "found matching")
                        if (secretData != null) {
                            Log.d("Transaction Observer", t.toString())
                            secretRevealed(secretData, entry.offerId)
                            break
                        }
                    }
                }
            }
        }
    }
}

class TransactionListenerEntry(val address: Address, val offerId: String, val script: Script)
