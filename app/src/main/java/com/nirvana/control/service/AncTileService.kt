package com.nirvana.control.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.nirvana.control.bluetooth.BluetrumSppManager
import com.nirvana.control.model.AncMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AncTileService : TileService() {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var sppManager: BluetrumSppManager

    override fun onCreate() {
        super.onCreate()
        sppManager = BluetrumSppManager.getInstance(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        sppManager.deviceState.onEach { state ->
            val tile = qsTile ?: return@onEach
            if (!state.isConnected) {
                tile.state = Tile.STATE_UNAVAILABLE
                tile.label = "ANC"
                tile.subtitle = "Disconnected"
            } else {
                tile.state = when (state.ancMode) {
                    AncMode.ANC_ON, AncMode.TRANSPARENCY -> Tile.STATE_ACTIVE
                    AncMode.OFF -> Tile.STATE_INACTIVE
                }
                tile.label = "ANC"
                tile.subtitle = state.ancMode.label
            }
            tile.updateTile()
        }.launchIn(scope)
    }

    override fun onClick() {
        super.onClick()
        val current = sppManager.deviceState.value.ancMode
        val next = when (current) {
            AncMode.OFF -> AncMode.ANC_ON
            AncMode.ANC_ON -> AncMode.TRANSPARENCY
            AncMode.TRANSPARENCY -> AncMode.OFF
        }
        sppManager.setAncMode(next)
    }
}
