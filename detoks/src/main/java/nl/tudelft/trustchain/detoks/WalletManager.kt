package nl.tudelft.trustchain.detoks

import android.content.SharedPreferences
import android.content.Context

class WalletManager(private val context: Context) {
    private val mWalletPrefs: SharedPreferences =
        context.getSharedPreferences(WALLET_PREFS_NAME, Context.MODE_PRIVATE)

    fun getOrCreateWallet(peerId: String): Wallet {
        val balance = mWalletPrefs.getFloat(peerId, 10.0f)
        return Wallet(balance)
    }

    fun setWalletBalance(peerId: String, balance: Float) {
        val editor = mWalletPrefs.edit()
        editor.putFloat(peerId, balance)
        editor.apply()
    }

    companion object {
        private const val WALLET_PREFS_NAME = "WalletPrefs"
    }
}

data class Wallet(var balance: Float = 10.0f)
