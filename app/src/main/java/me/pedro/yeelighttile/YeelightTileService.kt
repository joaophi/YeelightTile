package me.pedro.yeelighttile

import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket

private const val DEVICE_HOST = "luz-mesa"
private const val DEVICE_PORT = 55443

private const val POWER_MESSAGE = "{\"id\": 1, \"method\": \"get_prop\", \"params\": [\"power\"]}\r\n"
private const val TOGGLE_MESSAGE = "{\"id\": 1, \"method\": \"toggle\", \"params\": []}\r\n"

class YeelightTileService : TileService() {

    private lateinit var socket: Socket
    private lateinit var thread: Thread

    override fun onStartListening() {
        super.onStartListening()
        thread = Thread {
            val handler = Handler(Looper.getMainLooper())
            socket = Socket()
            socket.use {
                runCatching {
                    socket.connect(InetSocketAddress(DEVICE_HOST, DEVICE_PORT))
                }.onFailure {
                    handler.post {
                        qsTile.state = Tile.STATE_UNAVAILABLE
                        qsTile.updateTile()
                    }
                    return@Thread
                }

                val input = socket.getInputStream().bufferedReader()


                runCatching {
                    val output = socket.getOutputStream().bufferedWriter()
                    output.append(POWER_MESSAGE).flush()

                    val line = input.readLine()
                    val power = JSONObject(line)
                            .getJSONArray("result")
                            .getString(0) == "on"

                    handler.post {
                        qsTile.state = if (power) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                        qsTile.updateTile()
                    }
                }

                while (!socket.isClosed) runCatching {
                    val line = input.readLine()

                    val power = JSONObject(line)
                            .getJSONObject("params")
                            .getString("power") == "on"

                    handler.post {
                        qsTile.state = if (power) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                        qsTile.updateTile()
                    }
                }
            }
        }
        thread.start()
    }

    override fun onStopListening() {
        super.onStopListening()
        socket.close()
    }

    override fun onClick() {
        super.onClick()
        Thread {
            val handler = Handler(Looper.getMainLooper())
            runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(DEVICE_HOST, DEVICE_PORT))
                    val output = socket.getOutputStream().bufferedWriter()

                    output.append(TOGGLE_MESSAGE).flush()
                    handler.post {
                        qsTile.state = if (qsTile.state == Tile.STATE_INACTIVE) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                        qsTile.updateTile()
                    }
                }
            }.onFailure {
                handler.post {
                    Toast.makeText(applicationContext, it.message, Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}