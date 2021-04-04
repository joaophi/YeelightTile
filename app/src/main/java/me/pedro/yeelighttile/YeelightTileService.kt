package me.pedro.yeelighttile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONException
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

private const val DEVICE_HOST = "192.168.1.20"
private const val DEVICE_PORT = 55443

private const val POWER_MSG = "{\"id\": 1, \"method\": \"get_prop\", \"params\": [\"power\"]}\r\n"
private const val TOGGLE_MSG = "{\"id\": 1, \"method\": \"toggle\", \"params\": []}\r\n"

fun InetSocketAddress.powerStatusFlow(): Flow<Boolean> = flow {
    val socket = Socket()
    socket.soTimeout = 1_000
    socket.use {
        socket.connect(this@powerStatusFlow, 1_000)
        val input = socket.getInputStream().bufferedReader()

        try {
            val output = socket.getOutputStream().bufferedWriter()
            output.append(POWER_MSG).flush()

            val line = input.readLine()
            val power = JSONObject(line)
                .getJSONArray("result")
                .getString(0) == "on"

            emit(power)
        } catch (ex: SocketTimeoutException) {
        } catch (ex: JSONException) {
        }

        while (currentCoroutineContext().isActive) {
            yield()
            try {
                val line = input.readLine()

                val power = JSONObject(line)
                    .getJSONObject("params")
                    .getString("power") == "on"

                emit(power)
            } catch (ex: SocketTimeoutException) {
            } catch (ex: JSONException) {
            }
        }
    }
}.distinctUntilChanged()

class YeelightTileService : TileService() {

    private lateinit var scope: CoroutineScope

    private suspend fun setTileState(state: Int): Unit = withContext(Dispatchers.Main) {
        qsTile.state = state
        qsTile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        scope = CoroutineScope(context = SupervisorJob() + Dispatchers.IO)
        scope.launch {
            InetSocketAddress(DEVICE_HOST, DEVICE_PORT)
                .powerStatusFlow()
                .map { if (it) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE }
                .retryWhen { _, _ ->
                    emit(Tile.STATE_UNAVAILABLE)
                    delay(timeMillis = 1_000)
                    true
                }
                .collect(::setTileState)
        }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            try {
                val socket = Socket()
                socket.use { socket ->
                    socket.connect(InetSocketAddress(DEVICE_HOST, DEVICE_PORT))
                    val output = socket.getOutputStream().bufferedWriter()

                    output.append(TOGGLE_MSG).flush()

                    val state = when (qsTile.state) {
                        Tile.STATE_ACTIVE -> Tile.STATE_INACTIVE
                        Tile.STATE_INACTIVE -> Tile.STATE_ACTIVE
                        else -> Tile.STATE_UNAVAILABLE
                    }
                    setTileState(state)
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, ex.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        scope.cancel()
    }
}