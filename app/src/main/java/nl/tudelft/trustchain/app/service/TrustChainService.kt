package nl.tudelft.trustchain.app.service

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.android.service.IPv8Service
import nl.tudelft.trustchain.app.R
import nl.tudelft.trustchain.app.ui.dashboard.DashboardActivity

class TrustChainService : IPv8Service() {
    override fun onCreate() {
        super.onCreate()
        scope.launch {
            while (true) {
                updateNotification()
                delay(1000)
            }
        }
    }

    override fun createNotification(): NotificationCompat.Builder {
        val trustChainDashboardIntent = Intent(this, DashboardActivity::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent =
            TaskStackBuilder.create(this)
                .addNextIntentWithParentStack(trustChainDashboardIntent)
                .getPendingIntent(0, flags)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_CONNECTION)
            .setContentTitle("IPv8 Service")
            .setContentText("Connected to ${IPv8Android.getInstance().network.verifiedPeers.size} peers")
            .setSmallIcon(R.drawable.ic_device_hub_black_24dp)
            .setColor(getColor(R.color.colorPrimary))
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
    }
}
