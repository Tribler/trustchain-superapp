package nl.tudelft.ipv8.android.demo

import android.app.Application
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.IPv8Configuration
import nl.tudelft.ipv8.android.messaging.udp.AndroidUdpEndpoint
import nl.tudelft.ipv8.keyvault.CryptoProvider
import nl.tudelft.ipv8.keyvault.PrivateKey
import java.net.InetAddress

class IPv8Factory(
    private val application: Application
) {
    private var privateKey: PrivateKey? = null
    private var configuration: IPv8Configuration? = null
    private var cryptoProvider: CryptoProvider? = null

    fun setPrivateKey(key: PrivateKey): IPv8Factory {
        this.privateKey = key
        return this
    }

    fun setConfiguration(configuration: IPv8Configuration): IPv8Factory {
        this.configuration = configuration
        return this
    }

    fun setCryptoProvider(cryptoProvider: CryptoProvider): IPv8Factory {
        this.cryptoProvider = cryptoProvider
        return this
    }

    fun create(): IPv8 {
        val privateKey = privateKey ?: throw IllegalStateException("Private key is not set")
        val configuration = configuration ?: throw IllegalStateException("Configuration is not set")
        val cryptoProvider = cryptoProvider ?: throw IllegalStateException("CryptoProvider is not set")

        val connectivityManager = application.getSystemService<ConnectivityManager>()
            ?: throw IllegalStateException("ConnectivityManager not found")

        val endpoint = AndroidUdpEndpoint(8090, InetAddress.getByName("0.0.0.0"),
            connectivityManager)

        return IPv8(endpoint, configuration, privateKey, cryptoProvider)
    }
}
