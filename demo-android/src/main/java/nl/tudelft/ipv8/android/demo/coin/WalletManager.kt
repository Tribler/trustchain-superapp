package nl.tudelft.ipv8.android.demo.coin

import android.util.Log
import org.bitcoinj.core.DumpedPrivateKey
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.wallet.Wallet
import java.io.File


/**
 * The wallet manager which encapsulates the functionality of all possible interactions
 * with bitcoin wallets (including multi-signature wallets).
 * NOTE: Ideally should be separated from any Android UI concepts. Not the case currently.
 */
class WalletManager(walletManagerConfiguration: WalletManagerConfiguration, walletDir: File) {
    private val kit: WalletAppKit
    val params: NetworkParameters

    init {
        Log.i("Coin", "Coin: WalletManager attempting to start.")

        params = when (walletManagerConfiguration.network) {
            BitcoinNetworkOptions.TEST_NET -> TestNet3Params.get()
            BitcoinNetworkOptions.PRODUCTION -> TestNet3Params.get()
        }

        val filePrefix = when (walletManagerConfiguration.network) {
            BitcoinNetworkOptions.TEST_NET -> "forwarding-service-testnet"
            BitcoinNetworkOptions.PRODUCTION -> "forwarding-service"
        }

        kit = object : WalletAppKit(params, walletDir, filePrefix) {
            override fun onSetupCompleted() {
                // Make a fresh new key if no keys in stored wallet.
                if (wallet().keyChainGroupSize < 1) wallet().importKey(ECKey())
                Log.i("Coin", "Coin: WalletManager started successfully.")
            }
        }

        kit.startAsync()
    }

    fun getBalance(): Long {
        Log.e("Coin", kit.wallet().getBalance(Wallet.BalanceType.ESTIMATED).toFriendlyString())
        // TODO: Does not show correct value.
        return kit.wallet().getBalance(Wallet.BalanceType.ESTIMATED).value
    }

    fun getImportedKeyPairs(): MutableList<ECKey>? {
        return kit.wallet().importedKeys
    }

    fun importPrivateKey(privateKey: String) {
        kit.wallet().importKey(privateKeyStringToECKey(privateKey))
    }

    fun privateKeyStringToECKey(privateKey: String): ECKey {
        return DumpedPrivateKey.fromBase58(params, privateKey).key
    }

    fun ecKeyToPrivateKeyString(ecKey: ECKey): String {
        return ecKey.getPrivateKeyAsWiF(params)
    }

}
