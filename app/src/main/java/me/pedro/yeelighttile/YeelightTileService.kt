package me.pedro.yeelighttile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.github.joaophi.yeelight.Command
import com.github.joaophi.yeelight.Property
import com.github.joaophi.yeelight.YeelightDevice
import com.github.joaophi.yeelight.filter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.InetAddress

private const val HOSTNAME = "yeelink-light-strip1_miio77093229"

private fun reachableFlow(timeout: Long = 1_000): Flow<Boolean> = flow {
    val address = InetAddress.getByName(HOSTNAME)
    while (currentCoroutineContext().isActive) {
        emit(address.isReachable(timeout.toInt()))
        delay(timeout)
    }
}.retryWhen { _, _ ->
    emit(false)
    delay(timeout)
    true
}.distinctUntilChanged()

class YeelightTileService : TileService() {
    private lateinit var scope: CoroutineScope
    private lateinit var yeelight: YeelightDevice

    private fun updateState(state: Boolean?) {
        qsTile.state = when (state) {
            true -> Tile.STATE_ACTIVE
            false -> Tile.STATE_INACTIVE
            null -> Tile.STATE_UNAVAILABLE
        }
        qsTile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateState(state = null)
        scope = CoroutineScope(Dispatchers.IO)
        reachableFlow()
            .transformLatest { reachable ->
                emit(null)
                if (!reachable) return@transformLatest

                yeelight = YeelightDevice(HOSTNAME)
                yeelight.use {
                    val result = yeelight.sendCommand(Command.GetProp(Property.Power))
                    emit(result[Property.Power])
                    emitAll(yeelight.properties.filter(Property.Power))
                }
            }
            .retryWhen { _, _ ->
                emit(null)
                delay(1_000)
                true
            }
            .distinctUntilChanged()
            .onEach(::updateState)
            .launchIn(scope)
    }

    override fun onClick() {
        super.onClick()
        if (::yeelight.isInitialized) scope.launch {
            yeelight.sendCommand(Command.Toggle)
            val result = yeelight.sendCommand(Command.GetProp(Property.Power))

            updateState(state = result[Property.Power] ?: return@launch)
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        scope.cancel()
        updateState(state = null)
    }
}