package com.airstage.domain.service

import com.airstage.domain.model.DeviceStatus
import com.airstage.domain.model.FanSpeed
import com.airstage.domain.model.OperationMode
import com.airstage.domain.model.PowerState
import com.airstage.domain.model.Temperature
import com.airstage.domain.port.inbound.HeatPumpControlUseCase
import com.airstage.domain.port.outbound.HeatPumpGateway

class HeatPumpService(
    private val gateway: HeatPumpGateway,
) : HeatPumpControlUseCase {

    override suspend fun getStatus(): DeviceStatus = gateway.getStatus()

    override suspend fun turnOn() = gateway.setPower(PowerState.ON)

    override suspend fun turnOff() = gateway.setPower(PowerState.OFF)

    override suspend fun configure(
        temperature: Temperature?,
        mode: OperationMode?,
        fanSpeed: FanSpeed?,
    ) {
        // Applique dans un ordre logique : mode → ventilateur → température
        mode?.let { gateway.setMode(it) }
        fanSpeed?.let { gateway.setFanSpeed(it) }
        temperature?.let { gateway.setTemperature(it) }
    }
}
