package me.pedro.yeelighttile.yeelight

import me.pedro.yeelighttile.yeelight.Command.GetProp.Properties

private fun String.parseBoolean(): Boolean = when (this) {
    "on", "1" -> true
    "off", "0" -> false
    else -> throw Exception("error parsing boolean '$this'")
}

sealed class Command<R : Any>(val method: String) {
    open val params: List<Any> get() = emptyList()

    @Suppress("UNCHECKED_CAST")
    open fun parseResult(result: List<String>): R = (result.firstOrNull() == "ok") as R

    data class GetProp(
        val props: List<Property<*>>,
    ) : Command<Properties>(method = "get_prop") {
        constructor(vararg properties: Property<*>) : this(properties.toList())

        override val params: List<Any> get() = props.map(Property<*>::name)

        override fun parseResult(result: List<String>) =
            Properties(props.map(Property<*>::name).zip(result).toMap())

        sealed class Property<T>(val name: String, val parse: (String) -> T) {
            object Power : Property<Boolean>(name = "power", parse = String::parseBoolean)
        }

        data class Properties(private val map: Map<String, String>) {
            operator fun <T> get(property: Property<T>): T? =
                map[property.name]?.let(property.parse)
        }
    }

    object Toggle : Command<Boolean>(method = "toggle")
}