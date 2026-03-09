package com.airstage

import com.airstage.adapter.inbound.cli.buildClimCli
import com.airstage.adapter.outbound.airstage.AirStageLocalClient
import com.airstage.adapter.outbound.config.JsonFileConfigRepository
import com.airstage.domain.service.HeatPumpService
import com.github.ajalt.clikt.core.main

private const val DEFAULT_DEVICE_ID = "94BB43E75769"

fun main(args: Array<String>) {
    // Résolution de la config : CLI args > fichier ~/.airstage.json
    val configRepo = JsonFileConfigRepository()

    // Extraction des options globales --ip et --device-id avant de passer à Clikt
    val ip = args.getOptionValue("--ip")
    val deviceId = args.getOptionValue("--device-id")

    val fileConfig = configRepo.load()

    val resolvedIp = ip
        ?: fileConfig?.ip
        ?: error(
            "IP de la PAC non configurée.\n" +
                "Lancez d'abord : ./discover.sh\n" +
                "Ou spécifiez : --ip <adresse-ip>",
        )

    val resolvedDeviceId = deviceId
        ?: fileConfig?.deviceId
        ?: DEFAULT_DEVICE_ID

    val useHttps = fileConfig?.useHttps ?: true

    val gateway = AirStageLocalClient(
        host = resolvedIp,
        deviceId = resolvedDeviceId,
        useHttps = useHttps,
    )

    val useCase = HeatPumpService(gateway)
    val cli = buildClimCli(useCase)

    val filteredArgs = args.filterGlobalOptions("--ip", "--device-id")

    try {
        cli.main(filteredArgs)
    } finally {
        gateway.close()
    }
}

/**
 * Extrait la valeur d'une option de la forme ["--option", "valeur"] ou ["--option=valeur"].
 */
private fun Array<String>.getOptionValue(option: String): String? {
    val prefixed = "$option="
    for (i in indices) {
        if (this[i] == option && i + 1 < size) return this[i + 1]
        if (this[i].startsWith(prefixed)) return this[i].removePrefix(prefixed)
    }
    return null
}

/**
 * Retourne les args sans les options globales consommées (--option valeur ou --option=valeur).
 */
private fun Array<String>.filterGlobalOptions(vararg options: String): Array<String> {
    val result = mutableListOf<String>()
    var i = 0
    while (i < size) {
        val arg = this[i]
        val matchedOption = options.firstOrNull { arg == it || arg.startsWith("$it=") }
        if (matchedOption != null) {
            if (arg == matchedOption && i + 1 < size) i++ // skip value too
        } else {
            result.add(arg)
        }
        i++
    }
    return result.toTypedArray()
}
