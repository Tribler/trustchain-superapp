package com.example.musicdao.ui.screens.wallet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicdao.core.wallet.WalletService
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

    init {
        viewModelScope.launch {

            while (isActive) {
                publicKey.value = walletService.publicKey()
                estimatedBalance.value = walletService.estimatedBalance()
                status.value = walletService.walletStatus()
                syncProgress.value = walletService.percentageSynced()
                delay(REFRESH_DELAY)
            }
        }
    }

    fun requestFaucet() {
        walletService.defaultFaucetRequest()
    }

    companion object {
        const val REFRESH_DELAY = 1000L
    }
}

