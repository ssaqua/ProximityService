package ss.proximityservice

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class StartActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, ProximityService::class.java)
            .setAction(ProximityService.INTENT_ACTION_START)
        startService(intent)
        finish()
    }
}
