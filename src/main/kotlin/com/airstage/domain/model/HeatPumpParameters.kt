package com.airstage.domain.model

enum class PowerState { OFF, ON }

enum class OperationMode { AUTO, COOL, DRY, FAN, HEAT }

enum class FanSpeed { AUTO, QUIET, LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH, HIGH }

data class Temperature(val celsius: Double) {
    init {
        require(celsius in 10.0..30.0) {
            "Température hors limites : ${celsius}°C (plage autorisée : 10.0 - 30.0)"
        }
    }

    override fun toString(): String = String.format("%.1f°C", celsius)
}
