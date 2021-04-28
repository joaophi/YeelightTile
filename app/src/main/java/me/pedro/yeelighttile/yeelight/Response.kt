package me.pedro.yeelighttile.yeelight

import com.squareup.moshi.JsonClass

sealed class Response

data class Notification(
    val method: String,
    val params: Map<String, Any>,
) : Response()

sealed class Result(
    open val id: Int,
) : Response()

data class SuccessResult(
    override val id: Int,
    val result: List<String>,
) : Result(id)

data class ErrorResult(
    override val id: Int,
    val error: Error,
) : Result(id)

@JsonClass(generateAdapter = true)
data class Error(
    val code: Int,
    val message: String,
)