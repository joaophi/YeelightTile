package me.pedro.yeelighttile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import me.pedro.yeelighttile.yeelight.*
import me.pedro.yeelighttile.yeelight.Command.GetProp.Property
import java.net.InetAddress

private fun reachableFlow(timeout: Int = 1_000): Flow<Boolean> = flow {
    val address = InetAddress.getByName("luz-mesa")
    while (currentCoroutineContext().isActive)
        emit(address.isReachable(timeout))
}.retryWhen { _, _ -> emit(false);true }.distinctUntilChanged()

class YeelightTileService : TileService() {

    private lateinit var scope: CoroutineScope
    private lateinit var yeelight: YeelightDevice

    override fun onStartListening() {
        super.onStartListening()
        scope = CoroutineScope(Dispatchers.IO)
        reachableFlow()
            .transformLatest { reachable ->
                if (!reachable) {
                    emit(null)
                    return@transformLatest
                }

                yeelight = YeelightDevice(host = "luz-mesa")
                yeelight.use {
                    val result = yeelight.sendCommand(Command.GetProp(Property.Power))
                    emit(result[Property.Power])
                    emitAll(yeelight.properties.filter(Property.Power))
                }
            }
            .retry()
            .distinctUntilChanged()
            .map { state ->
                when (state) {
                    true -> Tile.STATE_ACTIVE
                    false -> Tile.STATE_INACTIVE
                    null -> Tile.STATE_UNAVAILABLE
                }
            }
            .onEach {
                qsTile.state = it
                qsTile.updateTile()
            }
            .launchIn(scope)
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            if (!::yeelight.isInitialized) return@launch

            yeelight.sendCommand(Command.Toggle)
            val result = yeelight.sendCommand(Command.GetProp(Property.Power))
            val state = when (result[Property.Power]) {
                true -> Tile.STATE_ACTIVE
                false -> Tile.STATE_INACTIVE
                null -> return@launch
            }
            qsTile.state = state
            qsTile.updateTile()
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        scope.cancel()
    }
}