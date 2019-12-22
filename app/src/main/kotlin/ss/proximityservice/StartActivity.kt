package ss.proximityservice

import android.app.Activity
import android.content.Intent
import android.os.Build

class StartActivity : Activity() {
    override fun onResume() {
        super.onResume()
        startProximityService()
        finish()
    }

    private fun startProximityService() {
        val intent = Intent(this, ProximityService::class.java)
            .setAction(ProximityService.INTENT_ACTION_START)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
