package me.pedro.yeelighttile

import com.squareup.moshi.*
import com.squareup.moshi.internal.Util
import java.lang.reflect.Type

class ApiJsonAdapter(moshi: Moshi) : JsonAdapter<Message>() {
    private val authRequestAdapter = moshi.adapter<Message.AuthRequest>()
    private val callServiceAdapter = moshi.adapter<Message.CallService>()
    private val getStatesAdapter = moshi.adapter<Message.GetStates>()
    private val subscribeToEventAdapter = moshi.adapter<Message.SubscribeToEvent>()

    override fun toJson(writer: JsonWriter, value: Message?) = when (value) {
        is Message.AuthRequest -> authRequestAdapter.toJson(writer, value)
        is Message.CallService -> callServiceAdapter.toJson(writer, value)
        is Message.GetStates -> getStatesAdapter.toJson(writer, value)
        is Message.SubscribeToEvent -> subscribeToEventAdapter.toJson(writer, value)
        else -> throw Exception("class inválida")
    }

    private val stringAdapter = moshi.adapter<String>()
    private val booleanAdapter = moshi.adapter<Boolean>()
    private val errorAdapter = moshi.adapter<Message.Result.Error.Data>()
    private val eventAdapter = moshi.adapter<Message.Event.EventData>()

    private val options: JsonReader.Options = JsonReader.Options
        .of("type", "message", "success", "error", "result", "event")

    override fun fromJson(reader: JsonReader): Message? {
        var type: String? = null
        var message: String? = null
        var success: Boolean? = null
        var error: Message.Result.Error.Data? = null
        var result: List<Message.Result.GetStates.State>? = null
        var event: Message.Event.EventData? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.selectName(options)) {
                0 -> type = stringAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("type", "type", reader)
                1 -> message = stringAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("message", "message", reader)
                2 -> success = booleanAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("success", "success", reader)
                3 -> error = errorAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("error", "error", reader)
                4 -> {
                    val value = reader.readJsonValue() as? List<*> ?: continue
                    result = value
                        .filterIsInstance<Map<*, *>>()
                        .mapNotNull {
                            Message.Result.GetStates.State(
                                entity_id = it["entity_id"] as? String ?: return@mapNotNull null,
                                state = it["state"] as? String ?: return@mapNotNull null,
                            )
                        }
                }
                5 -> event = eventAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("event", "event", reader)
                -1 -> {
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()

        type ?: throw Util.missingProperty("type", "type", reader)
        return when {
            success != null -> when {
                error != null -> Message.Result.Error(type, success, error)
                result != null -> Message.Result.GetStates(type, success, result)
                else -> {
                    val prop = if (success) "result" else "error"
                    throw Util.missingProperty(prop, prop, reader)
                }
            }
            event != null -> Message.Event(type, event)
            message != null -> Message.Auth(type, message)
            else -> Message.Auth(type)
        }
    }

    companion object : Factory {
        override fun create(
            type: Type,
            annotations: MutableSet<out Annotation>,
            moshi: Moshi
        ): JsonAdapter<*>? {
            if (annotations.isNotEmpty()) return null
            if (Types.getRawType(type) != Message::class.java) return null
            return ApiJsonAdapter(moshi)
        }
    }
}