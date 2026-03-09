package com.airstage.adapter.outbound.airstage

import com.airstage.domain.model.FanSpeed
import com.airstage.domain.model.OperationMode
import com.airstage.domain.model.PowerState
import com.airstage.domain.model.Temperature

object AirStageParameterMapper {

    // ---- Noms des paramètres API AirStage ----
    const val PARAM_POWER = "iu_onoff"
    const val PARAM_MODE = "iu_op_mode"
    const val PARAM_FAN_SPEED = "iu_fan_spd"
    const val PARAM_TARGET_TEMP = "iu_set_tmp"
    const val PARAM_INDOOR_TEMP = "iu_indoor_tmp"
    const val PARAM_OUTDOOR_TEMP = "iu_outdoor_tmp"
    const val PARAM_MODEL = "iu_model"

    val ALL_STATUS_PARAMS = listOf(
        PARAM_POWER,
        PARAM_MODE,
        PARAM_FAN_SPEED,
        PARAM_TARGET_TEMP,
        PARAM_INDOOR_TEMP,
        PARAM_OUTDOOR_TEMP,
        PARAM_MODEL,
    )

    private const val VALUE_NOT_SUPPORTED = "65535"

    // ---- PowerState ----

    fun toPowerCode(state: PowerState): String = when (state) {
        PowerState.OFF -> "0"
        PowerState.ON -> "1"
    }

    fun fromPowerCode(code: String): PowerState = when (code) {
        "1" -> PowerState.ON
        else -> PowerState.OFF
    }

    // ---- OperationMode ----

    fun toModeCode(mode: OperationMode): String = when (mode) {
        OperationMode.AUTO -> "0"
        OperationMode.COOL -> "1"
        OperationMode.DRY -> "2"
        OperationMode.FAN -> "3"
        OperationMode.HEAT -> "4"
    }

    fun fromModeCode(code: String): OperationMode = when (code) {
        "0" -> OperationMode.AUTO
        "1" -> OperationMode.COOL
        "2" -> OperationMode.DRY
        "3" -> OperationMode.FAN
        "4" -> OperationMode.HEAT
        else -> OperationMode.AUTO
    }

    // ---- FanSpeed ----

    fun toFanSpeedCode(speed: FanSpeed): String = when (speed) {
        FanSpeed.AUTO -> "0"
        FanSpeed.QUIET -> "2"
        FanSpeed.LOW -> "5"
        FanSpeed.MEDIUM_LOW -> "7"
        FanSpeed.MEDIUM -> "8"
        FanSpeed.MEDIUM_HIGH -> "9"
        FanSpeed.HIGH -> "11"
    }

    fun fromFanSpeedCode(code: String): FanSpeed = when (code) {
        "0" -> FanSpeed.AUTO
        "2" -> FanSpeed.QUIET
        "5" -> FanSpeed.LOW
        "7" -> FanSpeed.MEDIUM_LOW
        "8" -> FanSpeed.MEDIUM
        "9" -> FanSpeed.MEDIUM_HIGH
        "11" -> FanSpeed.HIGH
        else -> FanSpeed.AUTO
    }

    // ---- Temperature ----
    // API AirStage : valeur brute = celsius * 10
    // ex: 22.0°C → "220", 23.5°C → "235"

    fun toTemperatureRaw(temp: Temperature): String =
        (temp.celsius * 10).toInt().toString()

    fun fromTemperatureRaw(raw: String): Temperature {
        val rawInt = raw.toIntOrNull() ?: 220
        return Temperature(rawInt / 10.0)
    }

    // Temperature intérieure : (raw - 5000) / 100
    // ex: "7050" → (7050 - 5000) / 100 = 20.5°C
    fun fromIndoorTempRaw(raw: String): Double? {
        if (raw == VALUE_NOT_SUPPORTED) return null
        val rawInt = raw.toIntOrNull() ?: return null
        return (rawInt - 5000) / 100.0
    }

    // Temperature extérieure : même format que température intérieure (raw - 5000) / 100
    fun fromOutdoorTempRaw(raw: String): Double? {
        if (raw == VALUE_NOT_SUPPORTED) return null
        val rawInt = raw.toIntOrNull() ?: return null
        return (rawInt - 5000) / 100.0
    }
}
