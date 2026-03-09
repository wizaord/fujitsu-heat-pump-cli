package com.airstage.adapter.inbound.cli

import com.airstage.domain.model.FanSpeed
import com.airstage.domain.model.OperationMode
import com.airstage.domain.model.Temperature
import com.airstage.domain.port.inbound.HeatPumpControlUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import java.util.Locale
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.enum
import kotlinx.coroutines.runBlocking

class ClimCli(
    private val useCase: HeatPumpControlUseCase,
) : CliktCommand(name = "clim") {
    override fun help(context: Context) = "Contrôle de la pompe à chaleur Fujitsu AirStage (AGYG14KVCA)"
    override fun run() = Unit
}

class StatusCommand(
    private val useCase: HeatPumpControlUseCase,
) : CliktCommand(name = "status") {
    override fun help(context: Context) = "Affiche l'état actuel de la pompe à chaleur"

    override fun run() = runBlocking {
        val status = useCase.getStatus()

        echo("┌─────────────────────────────────────┐")
        echo("│       État de la PAC AirStage        │")
        echo("├─────────────────────────────────────┤")
        echo("│ Alimentation  : ${status.power.name.padEnd(20)} │")
        echo("│ Mode          : ${status.mode.name.padEnd(20)} │")
        echo("│ Ventilateur   : ${status.fanSpeed.name.padEnd(20)} │")
        echo("│ Temp. cible   : ${String.format(Locale.US, "%.1f°C", status.targetTemperature.celsius).padEnd(20)} │")

        val indoorStr = status.indoorTemperature
            ?.let { String.format(Locale.US, "%.1f°C", it) }
            ?: "N/A"
        echo("│ Temp. intér.  : ${indoorStr.padEnd(20)} │")

        val outdoorStr = status.outdoorTemperature
            ?.let { String.format(Locale.US, "%.1f°C", it) }
            ?: "N/A"
        echo("│ Temp. extér.  : ${outdoorStr.padEnd(20)} │")

        val modelStr = status.model ?: "N/A"
        echo("│ Modèle        : ${modelStr.padEnd(20)} │")
        echo("└─────────────────────────────────────┘")
    }
}

class OnCommand(
    private val useCase: HeatPumpControlUseCase,
) : CliktCommand(name = "on") {
    override fun help(context: Context) = "Allume la pompe à chaleur"

    override fun run() = runBlocking {
        useCase.turnOn()
        echo("Pompe à chaleur allumée.")
    }
}

class OffCommand(
    private val useCase: HeatPumpControlUseCase,
) : CliktCommand(name = "off") {
    override fun help(context: Context) = "Éteint la pompe à chaleur"

    override fun run() = runBlocking {
        useCase.turnOff()
        echo("Pompe à chaleur éteinte.")
    }
}

class SetCommand(
    private val useCase: HeatPumpControlUseCase,
) : CliktCommand(name = "set") {
    override fun help(context: Context) = "Modifie les réglages de la pompe à chaleur"

    private val temp: Double? by option(
        "--temp",
        help = "Température cible en °C (ex: 22.0, plage : 10.0-30.0)",
    ).double()

    private val mode: OperationMode? by option(
        "--mode",
        help = "Mode de fonctionnement : AUTO, COOL, DRY, FAN, HEAT",
    ).enum<OperationMode>()

    private val fan: FanSpeed? by option(
        "--fan",
        help = "Vitesse du ventilateur : AUTO, QUIET, LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH, HIGH",
    ).enum<FanSpeed>()

    override fun run() = runBlocking {
        if (temp == null && mode == null && fan == null) {
            echo("Aucun paramètre spécifié. Utilisez --temp, --mode ou --fan.")
            return@runBlocking
        }

        val temperature = temp?.let { Temperature(it) }
        useCase.configure(temperature = temperature, mode = mode, fanSpeed = fan)

        val applied = buildList {
            mode?.let { add("Mode : ${it.name}") }
            fan?.let { add("Ventilateur : ${it.name}") }
            temp?.let { add("Température : ${String.format(Locale.US, "%.1f°C", it)}") }
        }
        applied.forEach { echo("$it appliqué.") }
    }
}

fun buildClimCli(useCase: HeatPumpControlUseCase): CliktCommand =
    ClimCli(useCase).subcommands(
        StatusCommand(useCase),
        OnCommand(useCase),
        OffCommand(useCase),
        SetCommand(useCase),
    )
