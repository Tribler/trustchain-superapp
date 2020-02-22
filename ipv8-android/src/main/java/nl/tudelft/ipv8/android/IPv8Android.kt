package nl.tudelft.ipv8.android

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import androidx.core.content.getSystemService
import androidx.lifecycle.ProcessLifecycleOwner
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.IPv8Configuration
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.android.messaging.udp.AndroidUdpEndpoint
import nl.tudelft.ipv8.android.service.IPv8Service
import nl.tudelft.ipv8.keyvault.CryptoProvider
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import java.net.InetAddress

object IPv8Android {
    private var ipv8: IPv8? = null
    internal var serviceClass: Class<out IPv8Service>? = null

    fun getInstance(): IPv8 {
        return ipv8 ?: throw IllegalStateException("IPv8 is not initialized")
    }

    class Factory(
        private val application: Application
    ) {
        private var privateKey: PrivateKey? = null
        private var configuration: IPv8Configuration? = null
        private var serviceClass: Class<out IPv8Service> = IPv8Service::class.java

        fun setPrivateKey(key: PrivateKey): Factory {
            this.privateKey = key
            return this
        }

        fun setConfiguration(configuration: IPv8Configuration): Factory {
            this.configuration = configuration
            return this
        }

        fun setServiceClass(serviceClass: Class<out IPv8Service>): Factory {
            this.serviceClass = serviceClass
            return this
        }

        fun init(): IPv8 {
            val ipv8 = create()

            if (!ipv8.isStarted()) {
                ipv8.start()
                startAndroidService(application)
            }

            IPv8Android.ipv8 = ipv8
            IPv8Android.serviceClass = serviceClass

            defaultCryptoProvider = AndroidCryptoProvider

            return ipv8
        }

        private fun create(): IPv8 {
            val privateKey = privateKey
                ?: throw IllegalStateException("Private key is not set")
            val configuration = configuration
                ?: throw IllegalStateException("Configuration is not set")

            val connectivityManager = application.getSystemService<ConnectivityManager>()
                ?: throw IllegalStateException("ConnectivityManager not found")

            val endpoint = AndroidUdpEndpoint(8090, InetAddress.getByName("0.0.0.0"),
                connectivityManager)

            return IPv8(endpoint, configuration, privateKey, AndroidCryptoProvider)
        }

        private fun startAndroidService(context: Context) {
            val serviceIntent = Intent(context, serviceClass)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
