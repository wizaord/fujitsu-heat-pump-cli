package com.airstage.adapter.outbound.airstage

import com.airstage.adapter.outbound.airstage.AirStageParameterMapper.ALL_STATUS_PARAMS
import com.airstage.adapter.outbound.airstage.AirStageParameterMapper.PARAM_FAN_SPEED
import com.airstage.adapter.outbound.airstage.AirStageParameterMapper.PARAM_INDOOR_TEMP
import com.airstage.adapter.outbound.airstage.AirStageParameterMapper.PARAM_MODE
import com.airstage.adapter.outbound.airstage.AirStageParameterMapper.PARAM_MODEL
import com.airstage.adapter.outbound.airstage.AirStageParameterMapper.PARAM_OUTDOOR_TEMP
import com.airstage.adapter.outbound.airstage.AirStageParameterMapper.PARAM_POWER
import com.airstage.adapter.outbound.airstage.AirStageParameterMapper.PARAM_TARGET_TEMP
import com.airstage.domain.model.DeviceStatus
import com.airstage.domain.model.FanSpeed
import com.airstage.domain.model.OperationMode
import com.airstage.domain.model.PowerState
import com.airstage.domain.model.Temperature
import com.airstage.domain.port.outbound.HeatPumpGateway
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

class AirStageLocalClient(
    private val host: String,
    private val deviceId: String,
    useHttps: Boolean = true,
) : HeatPumpGateway {

    // Protocole actif, déterminé lors du premier appel réussi
    private var activeProtocol: String = if (useHttps) "https" else "http"
    private var protocolConfirmed = false

    private val client: HttpClient = HttpClient(CIO) {
        engine {
            https {
                // Désactive la validation SSL pour les certificats auto-signés
                trustManager = TrustAllCertificates
            }
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
    }

    // ---- API publique (implémentation de HeatPumpGateway) ----

    override suspend fun getStatus(): DeviceStatus {
        val values = getParameters(ALL_STATUS_PARAMS)
        return DeviceStatus(
            power = AirStageParameterMapper.fromPowerCode(values[PARAM_POWER] ?: "0"),
            mode = AirStageParameterMapper.fromModeCode(values[PARAM_MODE] ?: "0"),
            fanSpeed = AirStageParameterMapper.fromFanSpeedCode(values[PARAM_FAN_SPEED] ?: "0"),
            targetTemperature = AirStageParameterMapper.fromTemperatureRaw(values[PARAM_TARGET_TEMP] ?: "220"),
            indoorTemperature = values[PARAM_INDOOR_TEMP]?.let { AirStageParameterMapper.fromIndoorTempRaw(it) },
            outdoorTemperature = values[PARAM_OUTDOOR_TEMP]?.let { AirStageParameterMapper.fromOutdoorTempRaw(it) },
            model = values[PARAM_MODEL]?.takeIf { it.isNotBlank() },
        )
    }

    override suspend fun setPower(state: PowerState) {
        setParameter(PARAM_POWER, AirStageParameterMapper.toPowerCode(state))
    }

    override suspend fun setTemperature(temperature: Temperature) {
        setParameter(PARAM_TARGET_TEMP, AirStageParameterMapper.toTemperatureRaw(temperature))
    }

    override suspend fun setMode(mode: OperationMode) {
        setParameter(PARAM_MODE, AirStageParameterMapper.toModeCode(mode))
    }

    override suspend fun setFanSpeed(speed: FanSpeed) {
        setParameter(PARAM_FAN_SPEED, AirStageParameterMapper.toFanSpeedCode(speed))
    }

    fun close() = client.close()

    // ---- Implémentation interne ----

    private suspend fun getParameters(params: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        // L'adaptateur local retourne max 6 paramètres par requête
        params.chunked(6).forEachIndexed { index, batch ->
            if (index > 0) delay(1000L)
            val request = GetParamRequest(device_id = deviceId, list = batch)
            val response: GetParamResponse = postWithRetry("GetParam", request)
            result.putAll(response.value)
        }
        return result
    }

    private suspend fun setParameter(name: String, value: String) {
        val request = SetParamRequest(
            device_id = deviceId,
            value = mapOf(name to value),
        )
        postWithRetry<SetParamRequest, SetParamResponse>("SetParam", request)
    }

    private suspend inline fun <reified Req, reified Res> postWithRetry(
        endpoint: String,
        body: Req,
        maxRetries: Int = 5,
    ): Res {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                return doPost(endpoint, body)
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    // Si HTTPS a échoué et pas encore confirmé, essayer HTTP
                    if (!protocolConfirmed && activeProtocol == "https") {
                        activeProtocol = "http"
                    }
                    delay(1000L)
                }
            }
        }
        throw lastException ?: IllegalStateException("Échec après $maxRetries tentatives")
    }

    private suspend inline fun <reified Req, reified Res> doPost(
        endpoint: String,
        body: Req,
    ): Res {
        val url = "${activeProtocol}://${host}/${endpoint}"
        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        protocolConfirmed = true
        return response.body()
    }
}
