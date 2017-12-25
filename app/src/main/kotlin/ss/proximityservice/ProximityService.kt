package ss.proximityservice

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.widget.Toast
import ss.proximityservice.settings.SettingsActivity

class ProximityService : Service() {

    companion object {
        private const val TAG = "ProximityService"
        private const val NOTIFICATION_ID = 1

        const val INTENT_START_ACTION = "ss.proximityservice.START"
        const val INTENT_STOP_ACTION = "ss.proximityservice.STOP"

        var running = false
    }

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    // using deprecated KeyguardLock as the suggested alternative (WindowManager.LayoutParams flags)
    // is not suitable for a Service with no user interface
    private val keyguardLock: KeyguardManager.KeyguardLock by lazy {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.newKeyguardLock(TAG)
    }

    private val handler: Handler = Handler(Looper.getMainLooper())
    private var proximityWakeLock: PowerManager.WakeLock? = null
    private var keyguardDisableCount: Int = 0

    override fun onCreate() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= 21) {
            if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                proximityWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG)
            }
        } else {
            // PowerManager#getSupportedWakeLockFlags() removed so no WakeLock level support checking for api < 21
            proximityWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        when (action) {
            INTENT_START_ACTION -> start()
            INTENT_STOP_ACTION -> stop()
        }
        return Service.START_NOT_STICKY
    }

    private fun start() {
        proximityWakeLock?.let {
            if (it.isHeld) {
                handler.post {
                    Toast.makeText(applicationContext, "Proximity Service is already active", Toast.LENGTH_SHORT).show()
                }
            } else {
                handler.post {
                    Toast.makeText(applicationContext, "Proximity Service started", Toast.LENGTH_SHORT).show()
                }
                startForeground(NOTIFICATION_ID, stopNotification)
                running = true
                updateProximitySensorMode(true)
            }
        } ?: run {
            handler.post {
                Toast.makeText(applicationContext, "Proximity WakeLock not supported on this device", Toast.LENGTH_SHORT).show()
            }
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(SettingsActivity.INTENT_SET_ACTIVE_ACTION))
    }

    private fun stop() {
        running = false
        handler.post {
            Toast.makeText(applicationContext, "Proximity Service stopped", Toast.LENGTH_SHORT).show()
        }
        stopSelf()
        showStartNotification()
        updateProximitySensorMode(false)
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(SettingsActivity.INTENT_SET_INACTIVE_ACTION))
    }

    override fun onDestroy() {
        stop()
    }

    override fun onBind(intent: Intent): IBinder? {
        // binding not supported
        return null
    }

    private val stopNotification: Notification
        get() {
            val settingsIntent = PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, SettingsActivity::class.java),
                    0
            )
            val stopIntent = PendingIntent.getService(
                    this,
                    1,
                    Intent(this, ProximityService::class.java).setAction(INTENT_STOP_ACTION),
                    0
            )

            val notification = NotificationCompat.Builder(this, TAG)
                    .setSmallIcon(R.drawable.ic_screen_lock_portrait_white_24dp)
                    .setContentTitle(getString(R.string.app_name))
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)

            if (Build.VERSION.SDK_INT >= 16) {
                notification.setContentText(getString(R.string.notification_running))
                        .setContentIntent(stopIntent)
                        .addAction(R.drawable.ic_settings_black_24dp, getString(R.string.notification_action_settings), settingsIntent)
            } else {
                notification.setContentText(getString(R.string.notification_settings))
                        .setContentIntent(settingsIntent)
            }

            if (Build.VERSION.SDK_INT >= 21) {
                notification.setCategory(Notification.CATEGORY_SERVICE)
            }

            return notification.build()
        }

    private fun showStartNotification() {
        val startIntent = PendingIntent.getService(
                this,
                0,
                Intent(this, ProximityService::class.java).setAction(INTENT_START_ACTION),
                0
        )

        val notification = Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_screen_lock_portrait_white_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_stopped))
                .setContentIntent(startIntent)
                .setPriority(Notification.PRIORITY_LOW)

        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }

    private fun updateProximitySensorMode(on: Boolean) {
        proximityWakeLock?.let {
            synchronized(it) {
                if (on) {
                    if (!it.isHeld) {
                        it.acquire()
                        disableKeyguard()
                    }
                } else {
                    if (it.isHeld) {
                        it.release()
                        reenableKeyguard()
                    }
                }
            }
        }
    }

    private fun disableKeyguard() {
        synchronized(keyguardLock) {
            if (keyguardDisableCount++ == 0) {
                keyguardLock.disableKeyguard()
            }
        }
    }

    private fun reenableKeyguard() {
        synchronized(keyguardLock) {
            if (keyguardDisableCount > 0) {
                if (--keyguardDisableCount == 0) {
                    keyguardLock.reenableKeyguard()
                }
            }
        }
    }
}
