package com.nirvana.control.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.nirvana.control.bluetooth.BluetrumSppManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class BeastModeTileService : TileService() {
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
                tile.label = "BEAST™ Mode"
                tile.subtitle = "Disconnected"
            } else {
                tile.state = if (state.gameMode) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                tile.label = "BEAST™ Mode"
                tile.subtitle = if (state.gameMode) "60ms Low Latency" else "Off"
            }
            tile.updateTile()
        }.launchIn(scope)
    }

    override fun onClick() {
        super.onClick()
        val current = sppManager.deviceState.value.gameMode
        sppManager.setGameMode(!current)
    }
}
