package com.airstage.domain.model

data class DeviceStatus(
    val power: PowerState,
    val mode: OperationMode,
    val fanSpeed: FanSpeed,
    val targetTemperature: Temperature,
    val indoorTemperature: Double?,
    val outdoorTemperature: Double?,
    val model: String?,
)
