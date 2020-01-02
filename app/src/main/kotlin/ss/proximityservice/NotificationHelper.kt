package ss.proximityservice

import android.app.*
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import ss.proximityservice.settings.SettingsActivity

class NotificationHelper(context: Context) : ContextWrapper(context) {
    private val manager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.service_state_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }

    fun notify(id: Int, notification: Notification) {
        manager.notify(id, notification)
    }

    fun getRunningNotification(): Notification {
        return NotificationCompat.Builder(baseContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_screen_lock_portrait)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_running))
            .setContentIntent(getActivityIntent<StopActivity>(PendingIntent.FLAG_ONE_SHOT))
            .addAction(
                R.drawable.ic_settings,
                getString(R.string.notification_action_settings),
                getActivityIntent<SettingsActivity>(PendingIntent.FLAG_UPDATE_CURRENT)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .build()
    }

    fun getStoppedNotification(): Notification {
        return NotificationCompat.Builder(baseContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_screen_lock_portrait)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_stopped))
            .setContentIntent(getActivityIntent<StartActivity>(PendingIntent.FLAG_ONE_SHOT))
            .addAction(
                R.drawable.ic_settings,
                getString(R.string.notification_action_settings),
                getActivityIntent<SettingsActivity>(PendingIntent.FLAG_UPDATE_CURRENT)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .build()
    }

    private inline fun <reified T : Activity> getActivityIntent(flags: Int): PendingIntent {
        return PendingIntent.getActivity(
            baseContext,
            0,
            Intent(baseContext, T::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            flags
        )
    }

    companion object {
        private const val CHANNEL_ID = "proximityservice"
    }
}
