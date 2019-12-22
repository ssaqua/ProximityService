package ss.proximityservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ss.proximityservice.ProximityService.Companion.INTENT_NOTIFY_ACTIVE
import ss.proximityservice.ProximityService.Companion.INTENT_NOTIFY_INACTIVE

@RequiresApi(Build.VERSION_CODES.N)
class ToggleTileService : TileService() {

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateTile()
        }
    }

    override fun onClick() {
        val activityClass = if (ProximityService.isRunning) {
            StopActivity::class.java
        } else {
            StartActivity::class.java
        }
        startActivity(Intent(this, activityClass).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        updateTile()
    }

    override fun onTileAdded() {
        updateTile()
    }

    override fun onStartListening() {
        updateTile()
        val filter = IntentFilter().apply {
            addAction(INTENT_NOTIFY_ACTIVE)
            addAction(INTENT_NOTIFY_INACTIVE)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(stateReceiver, filter)
    }

    override fun onStopListening() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stateReceiver)
    }

    private fun updateTile() {
        val tile = qsTile
        tile.state = if (ProximityService.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
