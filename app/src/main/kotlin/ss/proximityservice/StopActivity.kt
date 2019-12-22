package ss.proximityservice

import android.app.Activity
import android.content.Intent
import android.os.Build

class StopActivity : Activity() {
    override fun onResume() {
        super.onResume()
        stopProximityService()
        finish()
    }

    private fun stopProximityService() {
        val intent = Intent(this, ProximityService::class.java)
            .setAction(ProximityService.INTENT_ACTION_STOP)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
