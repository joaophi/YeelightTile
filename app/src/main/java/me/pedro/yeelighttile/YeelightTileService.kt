package me.pedro.yeelighttile

import android.service.quicksettings.Tile
import android.widget.Toast
import androidx.lifecycle.*
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket

private const val HA_TARGET = "${BuildConfig.HA_DOMAIN}.${BuildConfig.HA_ENTITY}"

class YeelightTileService : LifecycleAwareTileService() {
    private var webSocket: WebSocket? = null
    private lateinit var adapter: JsonAdapter<Message>
    private fun WebSocket.send(message: Message) = send(adapter.toJson(message))

    override fun onCreate() {
        super.onCreate()

        val webSocketFlow = OkHttpClient.Builder().build()
            .newWebSocket(Request.Builder().url(BuildConfig.HA_URL).build())

        adapter = Moshi.Builder()
            .add(ApiJsonAdapter)
            .build()
            .adapter()

        lifecycleScope.launch {
            webSocketFlow
                .filterIsInstance<Event.Message>()
                .onEach { event ->
                    webSocket = event.webSocket
                    val message = runCatching { adapter.fromJson(event.text) }.getOrNull()
                        ?: return@onEach
                    when (message) {
                        is Message.Auth -> when (message.type) {
                            "auth_required" ->
                                event.webSocket.send(Message.AuthRequest(BuildConfig.HA_TOKEN))
                            "auth_ok" -> {
                                event.webSocket.send(Message.SubscribeToEvent(event_type = "state_changed"))
                                event.webSocket.send(Message.GetStates())
                            }
                            "auth_invalid" -> toast(message.message)
                        }
                        is Message.Event -> {
                            val target = message.event.data.new_state
                            if (target.entity_id == HA_TARGET)
                                updateTile(target.state)
                        }
                        is Message.Result.GetStates -> {
                            val target = message.result.find { it.entity_id == HA_TARGET }
                            updateTile(target?.state)
                        }
                        is Message.Result.Error -> toast(message.error.message)
                        else -> Unit
                    }
                }
                .onCompletion {
                    updateTile(state = null)
                    webSocket = null
                }
                .retry {
                    delay(10_000)
                    true
                }
                .flowWithLifecycle(lifecycle)
                .collect()
        }
    }

    override fun onClick() {
        super.onClick()
        val target = Message.CallService.Target(HA_TARGET)
        webSocket?.send(Message.CallService(BuildConfig.HA_DOMAIN, service = "toggle", target))
    }

    private fun toast(message: String?): Unit =
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()

    private fun updateTile(state: String?) {
        qsTile.state = when (state) {
            "on" -> Tile.STATE_ACTIVE
            "off" -> Tile.STATE_INACTIVE
            else -> Tile.STATE_UNAVAILABLE
        }
        qsTile.updateTile()
    }
}