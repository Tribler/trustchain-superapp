package nl.tudelft.trustchain.atomicswap.ui.wallet

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WalletViewModel : ViewModel() {

    val bitcoinBalance = MutableLiveData<String>()
    val ethereumBalance = MutableLiveData<String>()

    fun setBitcoinBalance(balance: String) {
        bitcoinBalance.postValue(balance)
    }

    fun setEthereumBalance(balance: String) {
        ethereumBalance.postValue(balance)
    }

}
