package ss.proximityservice

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class StopActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, ProximityService::class.java)
            .setAction(ProximityService.INTENT_ACTION_STOP)
        startService(intent)
        finish()
    }
}
