package ss.proximityservice

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.widget.Toast
import ss.proximityservice.settings.SettingsActivity
import java.util.concurrent.atomic.AtomicInteger

class ProximityService : Service() {
    private val proximityWakeLock: PowerManager.WakeLock? by lazy {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG)
            } else {
                null
            }
        } else {
            // no WakeLock level support checking for api < 21
            powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG)
        }
    }

    // using deprecated KeyguardLock as the suggested alternative (WindowManager.LayoutParams flags)
    // is not suitable for a Service with no user interface
    private val keyguardLock: KeyguardManager.KeyguardLock by lazy {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.newKeyguardLock(TAG)
    }

    private val keyguardDisableCount: AtomicInteger = AtomicInteger(0)

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val runningNotification: Notification
        get() {
            val stopIntent = PendingIntent.getService(this, 0,
                    Intent(this, ProximityService::class.java).setAction(INTENT_STOP_ACTION),
                    PendingIntent.FLAG_ONE_SHOT)
            val settingsIntent = PendingIntent.getActivity(this, 0,
                    Intent(this, SettingsActivity::class.java),
                    PendingIntent.FLAG_ONE_SHOT)

            val notification = NotificationCompat.Builder(this, CHANNEL_RUNNING)
                    .setSmallIcon(R.drawable.ic_screen_lock_portrait_white_24dp)
                    .setContentTitle(getString(R.string.app_name))
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                notification.setContentText(getString(R.string.notification_running))
                        .setContentIntent(stopIntent)
                        .addAction(R.drawable.ic_settings_white_24dp, getString(R.string.notification_action_settings), settingsIntent)
            } else {
                notification.setContentText(getString(R.string.notification_settings))
                        .setContentIntent(settingsIntent)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                notification.setCategory(Notification.CATEGORY_SERVICE)
            }

            return notification.build()
        }

    private val stoppedNotification: Notification
        get() {
            val startIntent = PendingIntent.getService(this, 0,
                    Intent(this, ProximityService::class.java).setAction(INTENT_START_ACTION),
                    PendingIntent.FLAG_ONE_SHOT)
            val settingsIntent = PendingIntent.getActivity(this, 0,
                    Intent(this, SettingsActivity::class.java),
                    PendingIntent.FLAG_ONE_SHOT)

            val notification = NotificationCompat.Builder(this, CHANNEL_STOPPED)
                    .setSmallIcon(R.drawable.ic_screen_lock_portrait_white_24dp)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.notification_stopped))
                    .setContentIntent(startIntent)
                    .setPriority(NotificationCompat.PRIORITY_LOW)

            if (Build.VERSION.SDK_INT >= 16) {
                notification.addAction(R.drawable.ic_settings_white_24dp, getString(R.string.notification_action_settings), settingsIntent)
            }

            return notification.build()
        }

    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        when (action) {
            INTENT_START_ACTION -> start()
            INTENT_STOP_ACTION -> stop()
        }
        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        if (running) stop()
    }

    // binding not supported
    override fun onBind(intent: Intent): IBinder? = null

    private fun start() {
        proximityWakeLock?.let {
            if (it.isHeld) {
                handler.post { toast("Proximity Service is already active") }
            } else {
                handler.post { toast("Proximity Service started") }
                startForeground(NOTIFICATION_ID, runningNotification)
                updateProximitySensorMode(true)
                running = true
                LocalBroadcastManager.getInstance(this)
                        .sendBroadcast(Intent(SettingsActivity.INTENT_SET_ACTIVE_ACTION))
            }
        } ?: run {
            handler.post { toast("Proximity WakeLock not supported on this device") }
        }
    }

    private fun stop() {
        handler.post { toast("Proximity Service stopped") }
        updateProximitySensorMode(false)
        running = false
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(Intent(SettingsActivity.INTENT_SET_INACTIVE_ACTION))
        stopSelf()
        // TODO: conditionally post the stopped notification
        notificationManager.notify(NOTIFICATION_ID, stoppedNotification)
    }

    private fun updateProximitySensorMode(on: Boolean) {
        proximityWakeLock?.let {
            synchronized(it) {
                if (on) {
                    if (!it.isHeld) {
                        it.acquire()
                        updateKeyguardMode(false)
                    }
                } else {
                    if (it.isHeld) {
                        it.release()
                        updateKeyguardMode(true)
                    }
                }
            }
        }
    }

    private fun updateKeyguardMode(on: Boolean) {
        synchronized(keyguardLock) {
            if (on) {
                if (keyguardDisableCount.get() > 0) {
                    if (keyguardDisableCount.decrementAndGet() == 0) {
                        keyguardLock.reenableKeyguard()
                    }
                }
            } else {
                if (keyguardDisableCount.getAndAdd(1) == 0) {
                    keyguardLock.disableKeyguard()
                }
            }
        }
    }

    private fun toast(text: String) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "ProximityService"

        const val INTENT_START_ACTION = "ss.proximityservice.START"
        const val INTENT_STOP_ACTION = "ss.proximityservice.STOP"

        private const val CHANNEL_RUNNING = "running"
        private const val CHANNEL_STOPPED = "stopped"

        private const val NOTIFICATION_ID = 1

        var running = false
    }
}
