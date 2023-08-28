package nl.tudelft.trustchain.app.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.annotation.RequiresApi
import nl.tudelft.ipv8.android.service.IPv8Service
import nl.tudelft.trustchain.app.R
import nl.tudelft.trustchain.app.ui.dashboard.DashboardActivity

@RequiresApi(Build.VERSION_CODES.M)
class TrustChainService : IPv8Service() {
    override fun createNotification(): NotificationCompat.Builder {
        val trustChainDashboardIntent = Intent(this, DashboardActivity::class.java)
        val flags = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else -> PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = TaskStackBuilder.create(this)
            .addNextIntentWithParentStack(trustChainDashboardIntent)
            .getPendingIntent(0, flags)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_CONNECTION)
            .setContentTitle("IPv8")
            .setContentText("Running")
            .setSmallIcon(R.drawable.ic_insert_link_black_24dp)
            .setContentIntent(pendingIntent)
    }
}
