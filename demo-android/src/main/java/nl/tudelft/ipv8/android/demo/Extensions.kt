package nl.tudelft.ipv8.android.demo

import android.content.Context
import android.content.Intent
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.android.demo.service.IPv8Service

private fun startAndroidService(context: Context) {
    val serviceIntent = Intent(context, IPv8Service::class.java)
    context.startForegroundService(serviceIntent)
}

fun IPv8.startIfNotRunning(context: Context) {
    if (!isStarted()) {
        start()
        startAndroidService(context)
    }
}
