package com.airstage.domain.port.inbound

import com.airstage.domain.model.DeviceStatus
import com.airstage.domain.model.FanSpeed
import com.airstage.domain.model.OperationMode
import com.airstage.domain.model.Temperature

interface HeatPumpControlUseCase {
    suspend fun getStatus(): DeviceStatus
    suspend fun turnOn()
    suspend fun turnOff()
    suspend fun configure(
        temperature: Temperature? = null,
        mode: OperationMode? = null,
        fanSpeed: FanSpeed? = null,
    )
}
