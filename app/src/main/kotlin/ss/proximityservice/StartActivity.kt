package ss.proximityservice

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class StartActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, ProximityService::class.java)
                .setAction(ProximityService.INTENT_START_ACTION))
        finish()
    }
}
