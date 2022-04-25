package nl.tudelft.trustchain.atomicswap

import nl.tudelft.trustchain.atomicswap.swap.WalletHolder
import org.bitcoinj.core.Address
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.listeners.OnTransactionBroadcastListener
import org.bitcoinj.script.Script

class SwapTransactionBroadcastListener(): OnTransactionBroadcastListener{

    // transaction list
    val watchedAddresses: MutableList<TransactionListenerEntry> = mutableListOf()

    // callback
    private lateinit var secretRevealed: (ByteArray, String) -> Unit

    fun setOnSecretRevealed(callback:(ByteArray, String) -> Unit){
        this.secretRevealed = callback
    }

    // add transactions
    fun addWatchedAddress(entry: TransactionListenerEntry){
        watchedAddresses.add(entry)
        WalletHolder.bitcoinWallet.addWatchedAddress(entry.address)
    }

    // listener
    override fun onTransaction(peer: org.bitcoinj.core.Peer?, t: Transaction?) {
        if(t!=null){

            val input = t.getInput(0)
            val script = input.scriptSig
            val secret = script.chunks[2]
            val secretData = secret.data
            val originalLockScript = script.chunks[4].data

            if(originalLockScript != null) {

                for(entry in watchedAddresses){
                    if(entry.script.program.contentEquals(originalLockScript)){

                        if (secretData != null) {
                            secretRevealed(secretData, entry.offerId)
                            watchedAddresses.remove(entry)
                            break
                        }
                    }
                }
            }
        }
    }
}

// class for entries
class TransactionListenerEntry(val address: Address, val offerId: String, val script: Script)
