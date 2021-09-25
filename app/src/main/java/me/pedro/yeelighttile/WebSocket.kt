package me.pedro.yeelighttile

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*

sealed interface Event {
    val webSocket: WebSocket

    data class Message(override val webSocket: WebSocket, val text: String) : Event
}

fun OkHttpClient.newWebSocket(request: Request): Flow<Event> = callbackFlow {
    val webSocket = newWebSocket(request, object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            trySend(Event.Message(webSocket, text))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            close()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            close(t)
        }
    })
    awaitClose { webSocket.close(code = 1000, reason = null) }
}