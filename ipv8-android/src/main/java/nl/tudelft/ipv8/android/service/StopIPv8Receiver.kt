package nl.tudelft.ipv8.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nl.tudelft.ipv8.android.IPv8Android

class StopIPv8Receiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, IPv8Android.serviceClass)
        context.stopService(serviceIntent)
    }
}
