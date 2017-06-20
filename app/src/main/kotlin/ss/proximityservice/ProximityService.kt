package ss.proximityservice

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.widget.Toast

class ProximityService : Service() {
    val tag = "ProximityService"
    val notificationId = 1

    val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    // using deprecated KeyguardLock as the suggested alternative (WindowManager.LayoutParams flags)
    // is not suitable for a Service with no user interface
    val keyguardLock: KeyguardManager.KeyguardLock by lazy {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.newKeyguardLock(tag)
    }

    val handler: Handler = Handler(Looper.getMainLooper())
    var proximityWakeLock: PowerManager.WakeLock? = null
    var keyguardDisableCount: Int = 0

    override fun onCreate() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= 21) {
            if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                proximityWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, tag)
            }
        } else {
            // PowerManager#getSupportedWakeLockFlags() removed so no WakeLock level support checking for api < 21
            proximityWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, tag)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        proximityWakeLock?.let {
            if (it.isHeld) {
                handler.post {
                    Toast.makeText(applicationContext, "Proximity WakeLock is already acquired", Toast.LENGTH_SHORT).show()
                }
            } else {
                handler.post {
                    Toast.makeText(applicationContext, "Proximity Service started", Toast.LENGTH_SHORT).show()
                }
                startForeground(notificationId, stopNotification)
                updateProximitySensorMode(true)
            }
        } ?: run {
            handler.post {
                Toast.makeText(applicationContext, "Proximity WakeLock not supported on this device", Toast.LENGTH_SHORT).show()
            }
        }

        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.post {
            Toast.makeText(applicationContext, "Proximity Service stopped", Toast.LENGTH_SHORT).show()
        }
        stopSelf()
        showStartNotification()
        updateProximitySensorMode(false)
    }

    override fun onBind(intent: Intent): IBinder? {
        // binding not supported
        return null
    }

    private val stopNotification: Notification
        get() {
            val stopIntent = PendingIntent.getActivity(this, 0, Intent(this, StopActivity::class.java), 0)

            val notification = Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_screen_lock_portrait_white_24dp)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.notification_running))
                    .setContentIntent(stopIntent)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_LOW)

            if (Build.VERSION.SDK_INT >= 21) {
                notification.setCategory(Notification.CATEGORY_SERVICE)
            }

            return notification.build()
        }

    private fun showStartNotification() {
        val startIntent = PendingIntent.getActivity(this, 0, Intent(this, StartActivity::class.java), 0)

        val notification = Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_screen_lock_portrait_white_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_stopped))
                .setContentIntent(startIntent)
                .setPriority(Notification.PRIORITY_LOW)

        notificationManager.notify(notificationId, notification.build())
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
