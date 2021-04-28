package me.pedro.yeelighttile.yeelight

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Request(
    val id: Int,
    val method: String,
    val params: List<Any>,
)