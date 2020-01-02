package ss.proximityservice

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.android.DaggerService
import ss.proximityservice.data.AppStorage
import ss.proximityservice.data.Mode
import ss.proximityservice.data.ProximityDetector
import ss.proximityservice.settings.NOTIFICATION_DISMISS
import ss.proximityservice.settings.OPERATIONAL_MODE
import ss.proximityservice.settings.SCREEN_OFF_DELAY
import ss.proximityservice.testing.OpenForTesting
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@OpenForTesting
class ProximityService : DaggerService(), ProximityDetector.ProximityListener {

    private val sensorManager: SensorManager by lazy {
        applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private val windowManager by lazy {
        applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val broadcastManager: LocalBroadcastManager by lazy {
        LocalBroadcastManager.getInstance(this)
    }

    private var proximityDetector: ProximityDetector? = null

    private var overlay: View? = null

    @SuppressLint("InlinedApi")
    private val overlayFlags = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN)

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

    private val notificationHelper: NotificationHelper by lazy {
        NotificationHelper(applicationContext)
    }

    private val mainHandler: Handler = Handler(Looper.getMainLooper())
    private val proximityHandler: Handler = Handler(Looper.myLooper())

    @Inject
    lateinit var appStorage: AppStorage

    override fun onCreate() {
        super.onCreate()

        proximityDetector = ProximityDetector(this)
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        sensor?.let {
            sensorManager.registerListener(proximityDetector, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
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
            val delay = when (appStorage.getInt(SCREEN_OFF_DELAY, 0)) {
                0 -> 0L
                1 -> 500L
                2 -> 1000L
                3 -> 1500L
                4 -> 2000L
                5 -> 2500L
                6 -> 3000L
                else -> 0L
            }
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
                startForeground(NOTIFICATION_ID, notificationHelper.getRunningNotification())
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
            notificationHelper.notify(NOTIFICATION_ID, notificationHelper.getStoppedNotification())
        }
    }

    private fun updateProximitySensorMode(on: Boolean) {
        when (appStorage.getInt(OPERATIONAL_MODE, Mode.DEFAULT.ordinal)) {
            Mode.DEFAULT.ordinal -> updateDefaultMode(on)
            Mode.AMOLED_WAKELOCK.ordinal -> updateAMOLEDMode(
                on,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
            Mode.AMOLED_NO_WAKELOCK.ordinal -> updateAMOLEDMode(on)
        }
    }

    private fun updateDefaultMode(on: Boolean) {
        overlay?.let {
            updateAMOLEDMode(false)
            updateKeyguardMode(true)
        }
        proximityWakeLock?.let { wakeLock ->
            synchronized(wakeLock) {
                if (on) {
                    if (!wakeLock.isHeld) {
                        wakeLock.acquire()
                        updateKeyguardMode(false)
                    }
                } else {
                    if (wakeLock.isHeld) {
                        wakeLock.release()
                        updateKeyguardMode(true)
                    }
                }
            }
        }
    }

    private fun updateAMOLEDMode(on: Boolean, flags: Int = 0) {
        proximityWakeLock?.let { wakeLock ->
            if (wakeLock.isHeld) {
                wakeLock.release()
                updateKeyguardMode(true)
            }
        }
        if (on && !checkDrawOverlaySetting()) return
        synchronized(this) {
            if (on) {
                if (overlay == null) {
                    overlay = FrameLayout(this).apply {
                        systemUiVisibility = overlayFlags
                        setTheme(R.style.OverlayTheme)
                    }
                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_SYSTEM_ALERT else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        flags,
                        PixelFormat.OPAQUE
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        params.layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }

                    // simulate SYSTEM_UI_FLAG_IMMERSIVE_STICKY for devices below API 19
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                        overlay?.setOnSystemUiVisibilityChangeListener { visibility ->
                            if ((visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                                proximityHandler.postDelayed({
                                    overlay?.systemUiVisibility = overlayFlags
                                }, 2000)
                            }
                        }
                    }
                    windowManager.addView(overlay, params)
                    updateKeyguardMode(true)
                }
            } else {
                overlay?.let {
                    it.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                    windowManager.removeView(it)
                    overlay = null
                }
                updateKeyguardMode(false)
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

    private fun checkDrawOverlaySetting(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val canDrawOverlays = Settings.canDrawOverlays(this)
        if (!canDrawOverlays) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            mainHandler.post { toast("AMOLED mode requires the permission for drawing over other apps / appear on top to be turned on") }
        }
        return canDrawOverlays
    }

    private fun toast(text: String) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "ProximityService:ProximitySensorWakeLock"

        const val INTENT_ACTION_START = "ss.proximityservice.START"
        const val INTENT_ACTION_STOP = "ss.proximityservice.STOP"

        const val INTENT_NOTIFY_ACTIVE = "ss.proximityservice.ACTIVE"
        const val INTENT_NOTIFY_INACTIVE = "ss.proximityservice.INACTIVE"

        private const val NOTIFICATION_ID = 1

        var isRunning = false
    }
}
