package nl.tudelft.trustchain.common.ethereum

import android.content.Context
import android.util.Log
import nl.tudelft.trustchain.common.ethereum.utils.generateWalletPassword
import org.web3j.crypto.ECKeyPair
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.io.File
import java.math.BigInteger
import java.security.SecureRandom

class EthereumWalletService {

    companion object {

        private var globalWeb3jWallet: EthereumWeb3jWallet? = null
        private var lastDir: File? = null

        fun createGlobalWeb3jWallet(context: Context): EthereumWeb3jWallet {
            Log.d(EthereumWalletService::class.simpleName, "Creating new Web3j Ethereum wallet")
            lastDir = context.cacheDir
            globalWeb3jWallet = createWeb3jWallet(context)
            return globalWeb3jWallet!!
        }

        fun getGlobalWeb3jWallet(context: Context): EthereumWeb3jWallet {
            if (globalWeb3jWallet != null) {
                Log.d(
                    EthereumWalletService::class.simpleName,
                    "Fetching already existing Web3j Ethereum wallet"
                )
                return globalWeb3jWallet!!
            }

            return createGlobalWeb3jWallet(context)
        }

        private fun createWeb3jWallet(context: Context): EthereumWeb3jWallet {
            // Connect to Web3j service
            Log.d(
                EthereumWalletService::class.simpleName,
                "Connecting to Web3j HTTP service (URL: ${BuildConfig.ETH_HTTP_URL})"
            )
            val web3j = Web3j.build(HttpService(BuildConfig.ETH_HTTP_URL))
            val clientVersion = web3j.web3ClientVersion().sendAsync().get().web3ClientVersion
            Log.d(
                EthereumWalletService::class.simpleName,
                "Connected with Web3j HTTP service (version: $clientVersion)"
            )

            // Create wallet
            globalWeb3jWallet = EthereumWeb3jWallet(
                web3j,
                context.cacheDir,
                getWalletKeys(context),
                getWalletPassword(context)
            )

            return globalWeb3jWallet!!
        }

        private const val SHARED_PREF_KEY_WALLET_PASSWORD = "web3j_wallet_password"
        private const val SHARED_PREF_KEY_PRIVATE_KEY = "web3j_wallet_private_key"
        private const val SHARED_PREF_KEY_PUBLIC_KEY = "web3j_wallet_public_key"

        private fun getWalletPassword(context: Context): String {
            val preferences = context.getSharedPreferences("web3j_wallet", Context.MODE_PRIVATE);

            var password = preferences.getString(SHARED_PREF_KEY_WALLET_PASSWORD, null)
            if (password == null) {
                password = generateWalletPassword(SecureRandom())
                Log.i(
                    EthereumWalletService::class.simpleName,
                    "Generated new password for the Web3j wallet: $password"
                )

                preferences.edit()
                    .putString(SHARED_PREF_KEY_WALLET_PASSWORD, password)
                    .apply()
            }

            return password
        }

        private fun getWalletKeys(context: Context): ECKeyPair {
            val preferences = context.getSharedPreferences("web3j_wallet", Context.MODE_PRIVATE);

            val privateKey = preferences.getString(SHARED_PREF_KEY_PRIVATE_KEY, null)
            val publicKey = preferences.getString(SHARED_PREF_KEY_PUBLIC_KEY, null)

            return if (privateKey == null || publicKey == null) {
                val keypair = ECKeyPair.create(BigInteger.valueOf(SecureRandom().nextLong()))
                Log.i(
                    EthereumWalletService::class.simpleName,
                    "Generated new keypair for the Web3j wallet (public key: ${keypair.publicKey})"
                )

                preferences.edit()
                    .putString(SHARED_PREF_KEY_PRIVATE_KEY, keypair.privateKey.toString())
                    .putString(SHARED_PREF_KEY_PUBLIC_KEY, keypair.publicKey.toString())
                    .apply()

                return keypair
            } else {
                ECKeyPair(BigInteger(privateKey), BigInteger(publicKey))
            }
        }

    }

}
