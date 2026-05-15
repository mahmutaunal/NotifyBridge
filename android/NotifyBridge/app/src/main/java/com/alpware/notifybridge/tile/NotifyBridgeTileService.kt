package com.alpware.notifybridge.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.alpware.notifybridge.R
import com.alpware.notifybridge.core.BridgeStateStore
import com.alpware.notifybridge.service.BridgeServiceController

/**
 * Quick Settings tile used to enable or disable notification forwarding.
 */
class NotifyBridgeTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    /**
     * Toggles the bridge state directly from the Quick Settings panel.
     */
    override fun onClick() {
        super.onClick()

        val currentState = BridgeStateStore.isBridgeEnabled(this)
        val newState = !currentState

        BridgeStateStore.setBridgeEnabled(this, newState)

        if (newState) {
            BridgeServiceController.start(this)
        } else {
            BridgeServiceController.stop(this)
        }

        updateTile()
    }

    /**
     * Refreshes the tile UI to reflect the current forwarding state.
     */
    private fun updateTile() {
        val enabled = BridgeStateStore.isBridgeEnabled(this)

        qsTile?.apply {
            state = if (enabled) {
                Tile.STATE_ACTIVE
            } else {
                Tile.STATE_INACTIVE
            }

            label = getString(R.string.app_name)
            subtitle = if (enabled) {
                getString(R.string.tile_status_active)
            } else {
                getString(R.string.tile_status_inactive)
            }

            updateTile()
        }
    }
}