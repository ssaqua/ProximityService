package ss.proximityservice

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.widget.Toast
import dagger.android.DaggerService
import ss.proximityservice.data.AppStorage
import ss.proximityservice.data.ProximityDetector
import ss.proximityservice.settings.NOTIFICATION_DISMISS
import ss.proximityservice.settings.SCREEN_OFF_DELAY
import ss.proximityservice.settings.SettingsActivity
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

class ProximityService : DaggerService(), ProximityDetector.ProximityListener {

    private val sensorManager by lazy { applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private var proximityDetector: ProximityDetector? = null

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
            val stopIntent = PendingIntent.getService(
                this, 0,
                Intent(this, ProximityService::class.java).setAction(INTENT_ACTION_STOP),
                PendingIntent.FLAG_ONE_SHOT
            )
            val settingsIntent = PendingIntent.getActivity(
                this, 0,
                Intent(this, SettingsActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_screen_lock_portrait_white_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setPriority(NotificationCompat.PRIORITY_LOW)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                notification.setContentText(getString(R.string.notification_running))
                    .setContentIntent(stopIntent)
                    .addAction(
                        R.drawable.ic_settings_white_24dp,
                        getString(R.string.notification_action_settings),
                        settingsIntent
                    )
            } else {
                notification.setContentText(getString(R.string.notification_settings))
                    .setContentIntent(settingsIntent)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // using CATEGORY_TRANSPORT instead of CATEGORY_SERVICE as a workaround for
                // Nova Launcher's notification badges and notification content in the popup
                // options (added in 5.2)
                notification.setCategory(Notification.CATEGORY_TRANSPORT)
            }

            return notification.build()
        }

    private val stoppedNotification: Notification
        get() {
            val startIntent = PendingIntent.getService(
                this, 0,
                Intent(this, ProximityService::class.java).setAction(INTENT_ACTION_START),
                PendingIntent.FLAG_ONE_SHOT
            )
            val settingsIntent = PendingIntent.getActivity(
                this, 0,
                Intent(this, SettingsActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_screen_lock_portrait_white_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_stopped))
                .setContentIntent(startIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)

            if (Build.VERSION.SDK_INT >= 16) {
                notification.addAction(
                    R.drawable.ic_settings_white_24dp,
                    getString(R.string.notification_action_settings),
                    settingsIntent
                )
            }

            return notification.build()
        }

    private val mainHandler: Handler = Handler(Looper.getMainLooper())
    private val proximityHandler: Handler = Handler(Looper.myLooper())

    private val broadcastManager: LocalBroadcastManager by lazy {
        LocalBroadcastManager.getInstance(this)
    }

    @Inject
    lateinit var appStorage: AppStorage

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Service State", NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        proximityDetector = ProximityDetector(this)
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        sensor?.let {
            sensorManager.registerListener(proximityDetector, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        when (action) {
            INTENT_ACTION_START -> start()
            INTENT_ACTION_STOP -> stop()
        }
        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        if (isRunning) stop()
        sensorManager.unregisterListener(proximityDetector)
    }

    // binding not supported
    override fun onBind(intent: Intent): IBinder? = null

    override fun onNear() {
        if (isRunning) {
            val delay = TimeUnit.SECONDS.toMillis(appStorage.getInt(SCREEN_OFF_DELAY, 0).toLong())
            proximityHandler.postDelayed({ updateProximitySensorMode(true) }, delay)
        }
    }

    override fun onFar() {
        if (isRunning) {
            proximityHandler.removeCallbacksAndMessages(null)
            updateProximitySensorMode(false)
        }
    }

    private fun start() {
        proximityWakeLock?.let {
            if (it.isHeld or isRunning) {
                mainHandler.post { toast("Proximity Service is already active") }
            } else {
                mainHandler.post { toast("Proximity Service started") }
                startForeground(NOTIFICATION_ID, runningNotification)
                isRunning = true
                broadcastManager.sendBroadcast(Intent(INTENT_NOTIFY_ACTIVE))
            }
        } ?: run {
            mainHandler.post { toast("Proximity WakeLock not supported on this device") }
        }
    }

    private fun stop() {
        mainHandler.post { toast("Proximity Service stopped") }
        updateProximitySensorMode(false)
        isRunning = false
        broadcastManager.sendBroadcast(Intent(INTENT_NOTIFY_INACTIVE))
        stopSelf()

        if (!appStorage.getBoolean(NOTIFICATION_DISMISS, true)) {
            notificationManager.notify(NOTIFICATION_ID, stoppedNotification)
        }
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

        const val INTENT_ACTION_START = "ss.proximityservice.START"
        const val INTENT_ACTION_STOP = "ss.proximityservice.STOP"

        const val INTENT_NOTIFY_ACTIVE = "ss.proximityservice.ACTIVE"
        const val INTENT_NOTIFY_INACTIVE = "ss.proximityservice.INACTIVE"

        private const val CHANNEL_ID = "proximityservice"

        private const val NOTIFICATION_ID = 1

        var isRunning = false
    }
}
