package com.airstage.adapter.outbound.airstage

import kotlinx.serialization.Serializable

@Serializable
data class GetParamRequest(
    val device_id: String,
    val device_sub_id: Int = 0,
    val req_id: String = "",
    val modified_by: String = "",
    val set_level: String = "03",
    val list: List<String>,
)

@Serializable
data class GetParamResponse(
    val result: String,
    val value: Map<String, String> = emptyMap(),
)

@Serializable
data class SetParamRequest(
    val device_id: String,
    val device_sub_id: Int = 0,
    val req_id: String = "",
    val modified_by: String = "",
    val set_level: String = "02",
    val value: Map<String, String>,
)

@Serializable
data class SetParamResponse(
    val result: String,
)
