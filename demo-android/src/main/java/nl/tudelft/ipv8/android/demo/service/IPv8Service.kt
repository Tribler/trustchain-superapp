package nl.tudelft.ipv8.android.demo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import kotlinx.coroutines.*
import nl.tudelft.ipv8.*
import nl.tudelft.ipv8.android.demo.DemoApplication
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.ui.peers.MainActivity

class IPv8Service : Service() {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val ipv8: IPv8 by lazy {
        (application as DemoApplication).ipv8
    }

    private var isBound = false

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        showForegroundNotification()
    }

    override fun onDestroy() {
        ipv8.stop()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        isBound = true
        showForegroundNotification()
        return LocalBinder()
    }

    override fun onRebind(intent: Intent?) {
        isBound = true
        showForegroundNotification()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isBound = false
        showForegroundNotification()
        return true
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_CONNECTION,
                getString(R.string.notification_channel_connection_title),
                importance
            )
            channel.description = getString(R.string.notification_channel_connection_description)
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService<NotificationManager>()
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun showForegroundNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val cancelBroadcastIntent = Intent(this, CancelIPv8Receiver::class.java)
        val cancelPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0, cancelBroadcastIntent, 0
        )

        val builder = NotificationCompat.Builder(this,
            NOTIFICATION_CHANNEL_CONNECTION)
            .setContentTitle("IPv8")
            .setContentText("Running")
            .setSmallIcon(R.drawable.ic_insert_link_black_24dp)
            .setContentIntent(pendingIntent)

        // Allow cancellation when the app is running in background
        if (!isBound) {
            builder.addAction(NotificationCompat.Action(0, "Stop", cancelPendingIntent))
        }

        startForeground(
            ONGOING_NOTIFICATION_ID,
            builder.build()
        )
    }

    inner class LocalBinder : Binder()

    companion object {
        const val NOTIFICATION_CHANNEL_CONNECTION = "service_notifications"
        private const val ONGOING_NOTIFICATION_ID = 1
    }
}
