package nl.tudelft.trustchain.musicdao.ui.screens.wallet

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import nl.tudelft.trustchain.musicdao.core.repositories.ArtistRepository
import nl.tudelft.trustchain.musicdao.core.wallet.UserWalletTransaction
import nl.tudelft.trustchain.musicdao.core.wallet.WalletService
import nl.tudelft.trustchain.musicdao.ui.SnackbarHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bitcoinj.core.Coin
import org.bitcoinj.wallet.Wallet
import javax.inject.Inject

@HiltViewModel
class BitcoinWalletViewModel @Inject constructor(val walletService: WalletService, val artistRepository: ArtistRepository) : ViewModel() {

    val publicKey: MutableStateFlow<String?> = MutableStateFlow(null)
    val confirmedBalance: MutableStateFlow<Coin?> = MutableStateFlow(null)
    val estimatedBalance: MutableStateFlow<String?> = MutableStateFlow(null)
    val status: MutableStateFlow<String?> = MutableStateFlow(null)
    val syncProgress: MutableStateFlow<Int?> = MutableStateFlow(null)
    val walletTransactions: MutableStateFlow<List<UserWalletTransaction>> = MutableStateFlow(listOf())

    val faucetInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isStarted: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {

        viewModelScope.launch {

            while (isActive) {
                syncProgress.value = walletService.percentageSynced()
                status.value = walletService.walletStatus()

                if (walletService.isStarted()) {
                    isStarted.value = true
                    publicKey.value = walletService.protocolAddress().toString()
                    estimatedBalance.value = walletService.estimatedBalance()
                    confirmedBalance.value = walletService.confirmedBalance()
                    walletTransactions.value = walletService.walletTransactions()
                }
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

    fun wallet(): Wallet {
        return walletService.wallet()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun donate(publicKey: String, amount: String): Boolean {
        val bitcoinPublicKey = artistRepository.getArtist(publicKey)?.bitcoinAddress ?: return false
        return walletService.sendCoins(bitcoinPublicKey, amount)
    }

    companion object {
        const val REFRESH_DELAY = 1000L
    }
}
