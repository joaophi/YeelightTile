package me.pedro.yeelighttile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.tinder.streamadapter.coroutines.CoroutinesStreamAdapterFactory
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import okhttp3.OkHttpClient

class YeelightTileService : TileService(), LifecycleOwner {
    private lateinit var api: HaApi

    override fun onCreate() {
        super.onCreate()

        val okHttpClient = OkHttpClient.Builder()
            .build()

        val scarlet = Scarlet.Builder()
            .webSocketFactory(okHttpClient.newWebSocketFactory(BuildConfig.HA_URL))
            .addMessageAdapterFactory(MoshiMessageAdapter.Factory())
            .addStreamAdapterFactory(CoroutinesStreamAdapterFactory())
            .lifecycle(
                AndroidLifecycle.ofLifecycleOwnerForeground(
                    application,
                    lifecycleOwner = this,
                    throttleTimeoutMillis = 0,
                )
            )
            .build()

        api = scarlet.create()

        lifecycleScope.launchWhenResumed {
            while (isActive) select<Unit> {
                api.authResponse.onReceive { authResponse ->
                    when (authResponse.type) {
                        "auth_required" -> api.sendAuth(HaApi.AuthRequest(BuildConfig.HA_TOKEN))
                        "auth_ok" -> {
                            api.subscribeToEvent(HaApi.SubscribeToEventRequest(eventType = "state_changed"))
                            api.getStates(HaApi.GetStatesRequest())
                        }
                    }
                }
                api.eventResponse.onReceive { eventResponse ->
                    val newState = eventResponse.event.data.newState
                    if (newState.entityId == "${BuildConfig.HA_DOMAIN}.${BuildConfig.HA_ENTITY}")
                        updateTile(newState.state)
                }
                api.getStates.onReceive { getStatesResponse ->
                    val state = getStatesResponse.result
                        .find { it.entityId == "${BuildConfig.HA_DOMAIN}.${BuildConfig.HA_ENTITY}" }
                        ?.state
                    updateTile(state)
                }
                api.authResponseInvalid.onReceive { toast(it.message) }
                api.errorResponse.onReceive { toast(it.error.message) }
            }
        }
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

    override fun onClick() {
        super.onClick()
        api.callService(
            HaApi.CallServiceRequest(
                BuildConfig.HA_DOMAIN,
                service = "toggle",
                target = HaApi.Target(entityId = "${BuildConfig.HA_DOMAIN}.${BuildConfig.HA_ENTITY}"),
            )
        )
    }

    override fun onStartListening() {
        super.onStartListening()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onStopListening() {
        super.onStopListening()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        updateTile(state = null)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    private val lifecycleRegistry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = lifecycleRegistry
}