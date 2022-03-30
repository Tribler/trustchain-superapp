package com.example.musicdao.ui.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicdao.core.wallet.WalletService
import com.example.musicdao.ui.SnackbarHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BitcoinWalletViewModel @Inject constructor(val walletService: WalletService) : ViewModel() {

    val publicKey: MutableStateFlow<String?> = MutableStateFlow(null)
    val confirmedBalance: MutableStateFlow<String?> = MutableStateFlow(null)
    val estimatedBalance: MutableStateFlow<String?> = MutableStateFlow(null)
    val status: MutableStateFlow<String?> = MutableStateFlow(null)
    val syncProgress: MutableStateFlow<Int?> = MutableStateFlow(null)

    val faucetInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        viewModelScope.launch {

            while (isActive) {
                publicKey.value = walletService.protocolAddress().toString()
                estimatedBalance.value = walletService.estimatedBalance()
                confirmedBalance.value = walletService.confirmedBalance()
                status.value = walletService.walletStatus()
                syncProgress.value = walletService.percentageSynced()
                delay(REFRESH_DELAY)
            }
        }
    }

    fun requestFaucet() {
        viewModelScope.launch {
            faucetInProgress.value = true
            val faucetRequestResult = walletService.defaultFaucetRequest()
            if (faucetRequestResult) {
                SnackbarHandler.displaySnackbar(text = "Successfully requested from faucet")
            } else {
                SnackbarHandler.displaySnackbar(text = "Something went wrong requesting from faucet")
            }
            faucetInProgress.value = false
        }
    }

    companion object {
        const val REFRESH_DELAY = 1000L
    }
}
