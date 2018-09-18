package ss.proximityservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.support.annotation.RequiresApi
import android.support.v4.content.LocalBroadcastManager
import ss.proximityservice.ProximityService.Companion.INTENT_NOTIFY_ACTIVE
import ss.proximityservice.ProximityService.Companion.INTENT_NOTIFY_INACTIVE
import ss.proximityservice.ProximityService.Companion.INTENT_ACTION_START
import ss.proximityservice.ProximityService.Companion.INTENT_ACTION_STOP

@RequiresApi(Build.VERSION_CODES.N)
class ToggleTileService : TileService() {

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateTile()
        }
    }

    override fun onClick() {
        val intent = Intent(this, ProximityService::class.java)
        intent.action = if (ProximityService.isRunning) INTENT_ACTION_STOP else INTENT_ACTION_START
        startService(intent)
        updateTile()
    }

    override fun onTileAdded() {
        updateTile()
    }

    override fun onStartListening() {
        updateTile()
        val filter = IntentFilter()
        filter.addAction(INTENT_NOTIFY_ACTIVE)
        filter.addAction(INTENT_NOTIFY_INACTIVE)
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
