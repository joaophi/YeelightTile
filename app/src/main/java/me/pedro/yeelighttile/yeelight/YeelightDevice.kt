package me.pedro.yeelighttile.yeelight

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import me.pedro.yeelighttile.yeelight.Command.GetProp.Properties
import okio.buffer
import okio.sink
import okio.source
import java.io.Closeable
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger

fun <V> Flow<Properties>.filter(property: Command.GetProp.Property<V>): Flow<V> =
    mapNotNull { it[property] }

class YeelightDevice(
    private val socket: Socket
) : Closeable {
    constructor(host: String, port: Int = 55443) : this(Socket(host, port))

    private val id = AtomicInteger()

    private val inputStream = socket.getInputStream()
    private val source = inputStream.source().buffer()
    private val sink = socket.sink().buffer()

    private val requestAdapter: JsonAdapter<Request>
    private val responseAdapter: JsonAdapter<Response>

    init {
        val moshi = Moshi.Builder()
            .add(ResponseJsonAdapter.Factory)
            .build()
        requestAdapter = moshi.adapter(Request::class.java)
        responseAdapter = moshi.adapter(Response::class.java)
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private val response = flow {
        while (currentCoroutineContext().isActive) when (inputStream.available()) {
            0 -> yield()
            else -> {
                val response = responseAdapter.fromJson(source) ?: continue
                emit(response)
            }
        }
    }.shareIn(scope, SharingStarted.WhileSubscribed())

    val properties = response
        .filterIsInstance<Notification>()
        .map { notification -> Properties(notification.params.mapValues { it.value.toString() }) }

    suspend fun <R : Any> sendCommand(command: Command<R>): R =
        withContext(scope.coroutineContext) {
            val id = id.getAndIncrement()
            val request = Request(id, command.method, command.params)
            requestAdapter.toJson(sink, request)
            sink.writeString("\r\n", Charsets.UTF_8)
            sink.flush()

            val response = response
                .filterIsInstance<Result>()
                .first { it.id == id }
            when (response) {
                is SuccessResult -> command.parseResult(response.result)
                is ErrorResult -> throw Exception(response.error.message)
            }
        }

    override fun close() {
        scope.cancel()
        socket.close()
    }
}