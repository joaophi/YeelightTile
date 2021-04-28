package me.pedro.yeelighttile.yeelight

import com.squareup.moshi.*
import com.squareup.moshi.internal.Util
import java.lang.reflect.Type

class ResponseJsonAdapter(moshi: Moshi) : JsonAdapter<Response>() {
    private val options: JsonReader.Options = JsonReader.Options.of("id", "result", "error", "method", "params")

    private val intAdapter: JsonAdapter<Int> = moshi.adapter(Int::class.java, emptySet(), "id")

    private val listOfStringAdapter: JsonAdapter<List<String>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, String::class.java), emptySet(), "result")

    private val errorAdapter: JsonAdapter<Error> = moshi.adapter(Error::class.java, emptySet(), "error")

    private val stringAdapter: JsonAdapter<String> = moshi.adapter(String::class.java, emptySet(), "method")

    private val mapAdapter: JsonAdapter<Map<String, Any>> = moshi.adapter(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
        emptySet(),
        "params"
    )

    override fun fromJson(reader: JsonReader): Response? {
        var id: Int? = null
        var result: List<String>? = null
        var error: Error? = null
        var method: String? = null
        var params: Map<String, Any>? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.selectName(options)) {
                0 -> id = intAdapter.fromJson(reader) ?: throw Util.unexpectedNull("id", "id", reader)
                1 -> result = listOfStringAdapter.fromJson(reader) ?: throw Util.unexpectedNull("result", "result", reader)
                2 -> error = errorAdapter.fromJson(reader) ?: throw Util.unexpectedNull("error", "error", reader)
                3 -> method = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull("method", "method", reader)
                4 -> params = mapAdapter.fromJson(reader) ?: throw Util.unexpectedNull("params", "params", reader)
                -1 -> {
                    // Unknown name, skip it.
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()

        return when {
            id != null -> when {
                result != null -> SuccessResult(id, result)
                error != null -> ErrorResult(id, error)
                else -> throw Util.missingProperty("result|error", "result|error", reader)
            }
            else -> Notification(
                method ?: throw Util.missingProperty("method", "method", reader),
                params ?: throw Util.missingProperty("params", "params", reader),
            )
        }
    }

    override fun toJson(writer: JsonWriter, value: Response?) {
        if (value == null) {
            throw NullPointerException("value was null! Wrap in .nullSafe() to write nullable values.")
        }

        writer.beginObject()
        when (value) {
            is Notification -> {
                writer.name("method")
                stringAdapter.toJson(writer, value.method)
                writer.name("params")
                mapAdapter.toJson(writer, value.params)
            }
            is Result -> {
                writer.name("id")
                intAdapter.toJson(writer, value.id)
                when (value) {
                    is SuccessResult -> {
                        writer.name("result")
                        listOfStringAdapter.toJson(writer, value.result)
                    }
                    is ErrorResult -> {
                        writer.name("error")
                        errorAdapter.toJson(writer, value.error)
                    }
                }
            }
        }
        writer.endObject()
    }

    companion object Factory : JsonAdapter.Factory {
        override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? {
            val rawType = Types.getRawType(type)
            return when {
                annotations.isNotEmpty() -> null
                rawType != Response::class.java -> null
                else -> ResponseJsonAdapter(moshi).nullSafe()
            }
        }
    }
}