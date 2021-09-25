package me.pedro.yeelighttile

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import kotlinx.coroutines.channels.ReceiveChannel

private var ID = 1

interface HaApi {
    // AUTH

    @JsonClass(generateAdapter = true)
    data class AuthRequest(
        @Json(name = "access_token") val accessToken: String,
        val type: String = "auth",
    )

    @Send
    fun sendAuth(authMessage: AuthRequest)

    @JsonClass(generateAdapter = true)
    data class AuthResponse(val type: String, @Json(name = "ha_version") val haVersion: String)

    @get:Receive
    val authResponse: ReceiveChannel<AuthResponse>

    @JsonClass(generateAdapter = true)
    data class AuthResponseInvalid(val message: String)

    @get:Receive
    val authResponseInvalid: ReceiveChannel<AuthResponseInvalid>

    // ERROR

    @JsonClass(generateAdapter = true)
    data class Error(val message: String)

    @JsonClass(generateAdapter = true)
    data class ErrorResponse(val error: Error)

    @get:Receive
    val errorResponse: ReceiveChannel<ErrorResponse>

    // EVENT

    @JsonClass(generateAdapter = true)
    data class SubscribeToEventRequest(
        @Json(name = "event_type")
        val eventType: String,
        val id: Int = ID++,
        val type: String = "subscribe_events",
    )

    @Send
    fun subscribeToEvent(subscribeToEventRequest: SubscribeToEventRequest)

    @JsonClass(generateAdapter = true)
    data class Data(@Json(name = "new_state") val newState: State)

    @JsonClass(generateAdapter = true)
    data class Event(val data: Data)

    @JsonClass(generateAdapter = true)
    data class EventResponse(val event: Event)

    @get:Receive
    val eventResponse: ReceiveChannel<EventResponse>

    // SERVICE

    @JsonClass(generateAdapter = true)
    data class Target(@Json(name = "entity_id") val entityId: String)

    @JsonClass(generateAdapter = true)
    data class CallServiceRequest(
        val domain: String,
        val service: String,
        val target: Target,
        val id: Int = ID++,
        val type: String = "call_service",
    )

    @Send
    fun callService(callServiceRequest: CallServiceRequest)

    // STATE

    @JsonClass(generateAdapter = true)
    data class GetStatesRequest(val type: String = "get_states", val id: Int = ID++)

    @Send
    fun getStates(getStatesRequest: GetStatesRequest)

    @JsonClass(generateAdapter = true)
    data class State(@Json(name = "entity_id") val entityId: String, val state: String)

    @JsonClass(generateAdapter = true)
    data class GetStatesResponse(val result: List<State>)

    @get:Receive
    val getStates: ReceiveChannel<GetStatesResponse>
}