package com.airstage.domain.port.outbound

data class DeviceConfig(
    val ip: String,
    val deviceId: String,
    val useHttps: Boolean = true,
)

interface DeviceConfigRepository {
    fun load(): DeviceConfig?
    fun save(config: DeviceConfig)
}
