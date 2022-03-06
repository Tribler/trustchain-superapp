package nl.tudelft.trustchain.atomicswap.ui.wallet

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WalletViewModel : ViewModel() {

    val bitcoinBalance = MutableLiveData<String>()
    val ethereumBalance = MutableLiveData<String>()

    fun setBitcoinBalance(balance: String) {
        bitcoinBalance.value = balance
    }

    fun setEthereumBalance(balance: String) {
        ethereumBalance.value = balance
    }

}
