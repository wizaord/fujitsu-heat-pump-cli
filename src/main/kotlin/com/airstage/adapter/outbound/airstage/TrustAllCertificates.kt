package com.airstage.adapter.outbound.airstage

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * TrustManager qui accepte tous les certificats SSL sans validation.
 * Nécessaire car l'adaptateur WiFi AirStage utilise un certificat auto-signé.
 */
object TrustAllCertificates : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
