package nl.tudelft.trustchain.eurotoken.view

import androidx.lifecycle.*
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import kotlinx.coroutines.launch

class WalletViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    private val _balance = MutableLiveData<Long>()
    val balance: LiveData<Long> get() = _balance

    //TODO can potentailly be deleted
    // might be handy for later transactions
    // now solely for test
    init {
        // load initial balance when ViewModel is created
        refreshBalance()
    }

    fun getPublicKey(): PublicKey {
        return defaultCryptoProvider.keyFromPublicBin(
            transactionRepository.trustChainCommunity.myPeer.publicKey.keyToBin()
        )
    }

    fun refreshBalance() {
        _balance.value = transactionRepository.getMyVerifiedBalance()
    }

    fun sendAmount(amount: Int, recipientPK: PublicKey, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // has to propgate--> might take longer..
                val success = transactionRepository.sendTransferProposal(
                    recipientPK.keyToBin(),
                    amount.toLong()
                )

                if (success) {
                    refreshBalance()
                }

                onComplete(success)
            } catch (e: Exception) {
                // Log error and notify caller
                // TODO
                onComplete(false)
            }
        }
    }

    // noa callback
    suspend fun sendAmount(amount: Int, recipientPK: PublicKey): Boolean {
        return try {
            val result = transactionRepository.sendTransferProposal(
                recipientPK.keyToBin(),
                amount.toLong()
            )

            if (result) {
                refreshBalance()
            }

            result
        } catch (e: Exception) {
            // should probably log this somewhere
            false
        }
    }

    // transaction history?
}
