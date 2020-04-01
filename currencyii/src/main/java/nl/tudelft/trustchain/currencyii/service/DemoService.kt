package nl.tudelft.trustchain.currencyii.service

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import nl.tudelft.trustchain.currencyii.ui.peers.MainActivity
import nl.tudelft.ipv8.android.service.IPv8Service
import nl.tudelft.trustchain.currencyii.R

class DemoService : IPv8Service() {
    override fun createNotification(): NotificationCompat.Builder {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0,
            notificationIntent, 0)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_CONNECTION)
            .setContentTitle("TrustChain Explorer")
            .setContentText("Running")
            .setSmallIcon(R.drawable.ic_insert_link_black_24dp)
            .setContentIntent(pendingIntent)
    }
}
