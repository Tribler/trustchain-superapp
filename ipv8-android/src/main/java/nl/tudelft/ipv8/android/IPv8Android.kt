package nl.tudelft.ipv8.android

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ProcessLifecycleOwner
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.android.service.IPv8Service

object IPv8Android {
    private lateinit var application: Application
    private lateinit var ipv8: IPv8
    internal lateinit var serviceClass: Class<out IPv8Service>

    fun init(application: Application, ipv8: IPv8AndroidFactory, serviceClass: Class<out IPv8Service>) {
        this.application = application
        this.ipv8 = ipv8.setCryptoProvider(AndroidCryptoProvider).create()
        this.serviceClass = serviceClass
    }

    fun getInstance(): IPv8 {
        if (!::ipv8.isInitialized) throw IllegalStateException("IPv8 is not initialized")

        if (!ipv8.isStarted()) {
            ipv8.start()
            startAndroidService(application)
        }

        return ipv8
    }

    private fun startAndroidService(context: Context) {
        val serviceIntent = Intent(context, serviceClass)
        context.startService(serviceIntent)
    }
}
