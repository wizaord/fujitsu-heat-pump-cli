package com.airstage.adapter.outbound.config

import com.airstage.domain.port.outbound.DeviceConfig
import com.airstage.domain.port.outbound.DeviceConfigRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class JsonFileConfigRepository(
    private val configFile: File = File(System.getProperty("user.home"), ".airstage.json"),
) : DeviceConfigRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override fun load(): DeviceConfig? {
        if (!configFile.exists()) return null
        return try {
            val dto = json.decodeFromString<DeviceConfigDto>(configFile.readText())
            DeviceConfig(
                ip = dto.ip,
                deviceId = dto.deviceId,
                useHttps = dto.useHttps,
            )
        } catch (_: Exception) {
            null
        }
    }

    override fun save(config: DeviceConfig) {
        val dto = DeviceConfigDto(
            ip = config.ip,
            deviceId = config.deviceId,
            useHttps = config.useHttps,
        )
        configFile.writeText(json.encodeToString(dto))
    }

    @Serializable
    private data class DeviceConfigDto(
        val ip: String,
        val deviceId: String,
        val useHttps: Boolean = true,
    )
}
