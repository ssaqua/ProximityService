package ss.proximityservice;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.widget.Toast;

public class ProximityService extends Service {

    private static final String TAG = "ProximityService";

    // identifier for persistent notification
    private static final int NOTIFICATION = 0;

    private NotificationManager notificationManager;

    private PowerManager.WakeLock proximityWakeLock;

    // using deprecated KeyguardLock as the suggested alternative (WindowManager.LayoutParams flags)
    // is not suitable for a Service with no user interface
    private KeyguardManager.KeyguardLock keyguardLock;

    private int keyguardDisableCount;

    private Handler handler;

    @Override
    public void onCreate() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        if (Build.VERSION.SDK_INT >= 21) {
            if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                proximityWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG);
            }
        } else {
            // PowerManager#getSupportedWakeLockFlags() removed so no WakeLock level support checking for api < 21
            proximityWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG);
        }

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        keyguardLock = keyguardManager.newKeyguardLock(TAG);

        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (proximityWakeLock != null) {
            if (proximityWakeLock.isHeld()) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Proximity WakeLock is already acquired",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Proximity Service started",
                                Toast.LENGTH_SHORT).show();
                    }
                });

                showStopNotification();
                updateProximitySensorMode(true);
            }
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Proximity WakeLock not supported on this device",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),
                        "Proximity Service stopped",
                        Toast.LENGTH_SHORT).show();
            }
        });

        showStartNotification();
        updateProximitySensorMode(false);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // binding not supported
        return null;
    }

    private void showStopNotification() {
        // PendingIntent from getBroadcast to prevent collapsing the notification drawer on stop
        PendingIntent stopIntent = PendingIntent.getBroadcast(this, 0, new Intent(ControlReceiver.ACTION_STOP), 0);

        Notification.Builder notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_screen_lock_portrait_white_24dp)
                .setContentTitle(getString(R.string.app_name))
                .addAction(R.drawable.empty, getString(R.string.action_stop), stopIntent)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setStyle(new Notification.BigTextStyle().bigText(getString(R.string.notification_running)));

        if (Build.VERSION.SDK_INT >= 21) {
            notification.setCategory(Notification.CATEGORY_SERVICE);
        }

        notificationManager.notify(NOTIFICATION, notification.build());
    }

    private void showStartNotification() {
        // PendingIntent from getActivity to collapse the notification drawer on start
        PendingIntent startIntent = PendingIntent.getActivity(this, 0, new Intent(getApplicationContext(), StartActivity.class), 0);

        Notification.Builder notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_screen_lock_portrait_white_24dp)
                .setContentTitle(getString(R.string.app_name))
                .addAction(R.drawable.empty, getString(R.string.action_restart), startIntent)
                .setPriority(Notification.PRIORITY_LOW)
                .setStyle(new Notification.BigTextStyle().bigText(getString(R.string.notification_stopped)));

        notificationManager.notify(NOTIFICATION, notification.build());
    }

    private void updateProximitySensorMode(boolean on) {
        synchronized (proximityWakeLock) {
            if (on) {
                if (!proximityWakeLock.isHeld()) {
                    proximityWakeLock.acquire();
                    disableKeyguard();
                }
            } else {
                if (proximityWakeLock.isHeld()) {
                    proximityWakeLock.release();
                    reenableKeyguard();
                }
            }
        }
    }

    private void disableKeyguard() {
        synchronized (keyguardLock) {
            if (keyguardDisableCount++ == 0) {
                keyguardLock.disableKeyguard();
            }
        }
    }

    private void reenableKeyguard() {
        synchronized (keyguardLock) {
            if (keyguardDisableCount > 0) {
                if (--keyguardDisableCount == 0) {
                    keyguardLock.reenableKeyguard();
                }
            }
        }
    }
}
