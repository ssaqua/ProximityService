package ss.proximityservice

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import ss.proximityservice.ProximityService.Companion.INTENT_ACTION_START
import ss.proximityservice.ProximityService.Companion.INTENT_ACTION_STOP

@RequiresApi(Build.VERSION_CODES.N)
class ToggleTileService : TileService() {

    override fun onClick() {
        val intent = Intent(this, ProximityService::class.java)
        intent.action = if (ProximityService.isRunning) INTENT_ACTION_STOP else INTENT_ACTION_START
        startService(intent)
    }

    override fun onTileAdded() {
        updateTile()
    }

    override fun onStartListening() {
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile
        tile.state = if (ProximityService.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
