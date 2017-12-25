package ss.proximityservice

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class StopActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProximityService.running = false
        startService(Intent(this, ProximityService::class.java)
                .setAction(ProximityService.INTENT_STOP_ACTION))
        finish()
    }
}
