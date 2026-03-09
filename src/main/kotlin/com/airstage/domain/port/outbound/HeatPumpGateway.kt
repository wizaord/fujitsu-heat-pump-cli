package com.airstage.domain.port.outbound

import com.airstage.domain.model.DeviceStatus
import com.airstage.domain.model.FanSpeed
import com.airstage.domain.model.OperationMode
import com.airstage.domain.model.PowerState
import com.airstage.domain.model.Temperature

interface HeatPumpGateway {
    suspend fun getStatus(): DeviceStatus
    suspend fun setPower(state: PowerState)
    suspend fun setTemperature(temperature: Temperature)
    suspend fun setMode(mode: OperationMode)
    suspend fun setFanSpeed(speed: FanSpeed)
}
