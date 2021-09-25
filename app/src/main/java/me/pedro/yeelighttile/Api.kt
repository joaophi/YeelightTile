package me.pedro.yeelighttile

import com.squareup.moshi.JsonClass

private var ID = 1

sealed interface Message {
    val type: String

    // REQUEST

    @JsonClass(generateAdapter = true)
    class AuthRequest(val access_token: String, override val type: String = "auth") : Message

    @JsonClass(generateAdapter = true)
    class SubscribeToEvent(
        val event_type: String,
        val id: Int = ID++,
        override val type: String = "subscribe_events",
    ) : Message

    @JsonClass(generateAdapter = true)
    class CallService(
        val domain: String,
        val service: String,
        val target: Target,
        val id: Int = ID++,
        override val type: String = "call_service"
    ) : Message {
        @JsonClass(generateAdapter = true)
        class Target(val entity_id: String)
    }

    @JsonClass(generateAdapter = true)
    class GetStates(val id: Int = ID++, override val type: String = "get_states") : Message

    // RESPONSES

    class Auth(override val type: String, val message: String? = null) : Message

    sealed interface Result : Message {
        val success: Boolean

        class Error(
            override val type: String,
            override val success: Boolean,
            val error: Data,
        ) : Result {
            @JsonClass(generateAdapter = true)
            class Data(val message: String)
        }

        class GetStates(
            override val type: String,
            override val success: Boolean,
            val result: List<State>,
        ) : Result {
            @JsonClass(generateAdapter = true)
            class State(val entity_id: String, val state: String)
        }
    }

    class Event(override val type: String, val event: EventData) : Message {
        @JsonClass(generateAdapter = true)
        class EventData(val data: Data) {
            @JsonClass(generateAdapter = true)
            class Data(val new_state: State) {
                @JsonClass(generateAdapter = true)
                class State(val entity_id: String, val state: String)
            }
        }
    }
}