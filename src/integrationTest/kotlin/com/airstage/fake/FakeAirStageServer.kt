package com.airstage.fake

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Faux serveur HTTP simulant l'API locale AirStage de la PAC Fujitsu.
 * Démarre sur un port aléatoire disponible pour les tests d'intégration.
 */
class FakeAirStageServer {

    // État interne simulé de la PAC
    private val deviceState = mutableMapOf(
        "iu_onoff" to "0",
        "iu_op_mode" to "4",       // HEAT
        "iu_fan_spd" to "0",       // AUTO
        "iu_set_tmp" to "220",     // 22.0°C
        "iu_indoor_tmp" to "7050", // 20.5°C
        "iu_outdoor_tmp" to "6800",  // (6800 - 5000) / 100 = 18.0°C
        "iu_model" to "AGYG14KVCA",
    )

    // Historique des requêtes SetParam reçues
    private val setRequests = CopyOnWriteArrayList<Map<String, String>>()

    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    fun start(): Int {
        val engine = embeddedServer(CIO, port = 0) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            routing {
                post("/GetParam") {
                    val request = call.receive<GetParamRequest>()
                    val values = request.list.associateWith { param ->
                        deviceState[param] ?: "65535"
                    }
                    call.respond(GetParamResponse(result = "OK", value = values))
                }

                post("/SetParam") {
                    val request = call.receive<SetParamRequest>()
                    request.value.forEach { (key, value) ->
                        deviceState[key] = value
                    }
                    setRequests.add(request.value.toMap())
                    call.respond(SetParamResponse(result = "OK"))
                }
            }
        }
        engine.start(wait = false)
        server = engine

        // Récupère le port attribué dynamiquement (resolvedConnectors est suspend)
        val connector = runBlocking { engine.engine.resolvedConnectors().first() }
        return connector.port
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 0, timeoutMillis = 500)
        server = null
        setRequests.clear()
    }

    // ---- Accesseurs pour les assertions de test ----

    fun getParameterValue(name: String): String? = deviceState[name]

    fun setParameterValue(name: String, value: String) {
        deviceState[name] = value
    }

    fun getSetRequests(): List<Map<String, String>> = setRequests.toList()

    fun getLastSetValue(paramName: String): String? =
        setRequests.lastOrNull { it.containsKey(paramName) }?.get(paramName)

    fun clearSetRequests() = setRequests.clear()

    // ---- DTO internes (pour désérialiser les requêtes reçues) ----

    @Serializable
    data class GetParamRequest(
        val device_id: String,
        val device_sub_id: Int = 0,
        val req_id: String = "",
        val modified_by: String = "",
        val set_level: String = "03",
        val list: List<String>,
    )

    @Serializable
    data class GetParamResponse(
        val result: String,
        val value: Map<String, String> = emptyMap(),
    )

    @Serializable
    data class SetParamRequest(
        val device_id: String,
        val device_sub_id: Int = 0,
        val req_id: String = "",
        val modified_by: String = "",
        val set_level: String = "02",
        val value: Map<String, String>,
    )

    @Serializable
    data class SetParamResponse(val result: String)
}
